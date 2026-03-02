package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import com.naal.bankmind.dto.Fraud.WhatIfRequestDto;
import com.naal.bankmind.dto.Fraud.WhatIfResponseDto;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Gender;
import com.naal.bankmind.entity.Localization;
import com.naal.bankmind.entity.Fraud.Category;
import com.naal.bankmind.entity.Fraud.CreditCards;
import com.naal.bankmind.entity.Fraud.FraudPredictions;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.repository.Fraud.CategoryRepository;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio para predicciones What-If (simulación de transacciones).
 *
 * Responsabilidad: buscar cliente por tarjeta, enriquecer datos,
 * llamar a la API de IA y opcionalmente persistir en BD.
 * La persistencia de predicciones está delegada a {@link PredictionMapper}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatIfService {

    private final CreditCardRepository creditCardRepository;
    private final FraudApiClient fraudApiClient;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PredictionMapper predictionMapper;
    private final FraudEmailService fraudEmailService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Par de entidades guardadas al persistir una simulación en BD.
     */
    private record SavedPair(OperationalTransactions transaction, FraudPredictions prediction) {
    }

    /**
     * Simula una predicción de fraude, con opción de guardar en BD.
     */
    @Transactional
    public WhatIfResponseDto simulatePrediction(WhatIfRequestDto request) {
        try {
            // 1. Buscar tarjeta y cliente
            CreditCards creditCard = creditCardRepository
                    .findByIdWithCustomerData(request.getCcNum())
                    .orElse(null);

            if (creditCard == null || creditCard.getCustomer() == null) {
                return errorResponse(false,
                        "No se encontró cliente para la tarjeta: " + request.getCcNum());
            }

            // 2. Validar que la tarjeta esté activa
            if (creditCard.getIsActive() == null || !creditCard.getIsActive()) {
                return errorResponse(true,
                        "TARJETA BLOQUEADA: Esta tarjeta ha sido bloqueada por seguridad. "
                                + "No se pueden procesar transacciones. "
                                + "Para más información, contacte a servicio al cliente: 1-800-BANKMIND");
            }

            Customer customer = creditCard.getCustomer();
            Localization location = customer.getLocalization();
            Gender gender = customer.getGender();

            // 3. Calcular edad
            Integer customerAge = customer.getDob() != null
                    ? Period.between(customer.getDob(), LocalDate.now()).getYears()
                    : null;

            // 4. Construir datetime simulado
            LocalDateTime simulatedDateTime = LocalDateTime.now()
                    .withHour(request.getHour() != null
                            ? request.getHour()
                            : LocalDateTime.now().getHour())
                    .withMinute(0).withSecond(0);

            // 5. Generar ID de transacción
            String prefix = Boolean.TRUE.equals(request.getSaveToDB()) ? "TRX-" : "WHATIF-";
            String transactionId = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // 6. Construir request para API de IA
            FraudPredictionRequestDto apiRequest = FraudPredictionRequestDto.builder()
                    .transactionId(transactionId)
                    .idCliente(customer.getIdCustomer().toString())
                    .transDateTransTime(simulatedDateTime.format(DATE_TIME_FORMATTER))
                    .amt(request.getAmt())
                    .category(request.getCategory())
                    .gender(predictionMapper.extractGenderCode(gender))
                    .job(customer.getJob() != null ? customer.getJob() : "Unknown")
                    .cityPop(location != null ? location.getCityPop() : 0)
                    .dob(customer.getDob() != null
                            ? customer.getDob().format(DATE_FORMATTER)
                            : "1990-01-01")
                    .lat(location != null ? location.getCustomerLat() : 0.0)
                    .lng(location != null ? location.getCustomerLong() : 0.0)
                    .merchLat(request.getMerchLat() != null ? request.getMerchLat() : 0.0)
                    .merchLong(request.getMerchLong() != null ? request.getMerchLong() : 0.0)
                    .build();

            // 7. Llamar a la API de IA
            FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(apiRequest);

            // 8. Si saveToDB, persistir transacción y predicción
            boolean savedToDB = false;
            if (Boolean.TRUE.equals(request.getSaveToDB())) {
                if (request.getMerchant() == null || request.getMerchant().trim().isEmpty()) {
                    return errorResponse(true,
                            "El campo 'merchant' es requerido para guardar en BD");
                }

                SavedPair saved = saveTransactionAndPrediction(
                        creditCard, request, simulatedDateTime, transactionId, apiResponse);
                savedToDB = true;

                if (FraudConstants.VEREDICTO_ALTO_RIESGO.equals(apiResponse.getVeredicto())) {
                    String email = creditCard.getCustomer().getEmail();
                    if (email != null && !email.trim().isEmpty()) {
                        fraudEmailService.sendFraudAlert(saved.transaction(), saved.prediction());
                    }
                } else {
                    saved.transaction().setStatus(FraudConstants.STATUS_APPROVED);
                    transactionRepository.save(saved.transaction());
                }
            }

            // 9. Construir respuesta enriquecida
            String customerLocation = location != null
                    ? location.getCity() + ", " + location.getState()
                    : null;

            return WhatIfResponseDto.builder()
                    .customerFound(true)
                    .customerName(customer.getFirstName() + " " + customer.getSurname())
                    .customerLocation(customerLocation)
                    .customerGender(gender != null ? gender.getGenderDescription() : null)
                    .customerAge(customerAge)
                    .simulatedAmount(request.getAmt())
                    .simulatedCategory(request.getCategory())
                    .simulatedHour(request.getHour())
                    .transactionId(transactionId)
                    .veredicto(apiResponse.getVeredicto())
                    .scoreFinal(apiResponse.getScoreFinal())
                    .detallesRiesgo(apiResponse.getDetallesRiesgo())
                    .datosAuditoria(apiResponse.getDatosAuditoria())
                    .recomendacion(apiResponse.getRecomendacion())
                    .savedToDB(savedToDB)
                    .build();

        } catch (Exception e) {
            log.error("Error simulando predicción What-If: {}", e.getMessage(), e);
            return errorResponse(false, "Error al simular predicción: " + e.getMessage());
        }
    }

    /**
     * Busca información del cliente por número de tarjeta (para mostrar antes de
     * simular).
     */
    public WhatIfResponseDto lookupCustomer(Long ccNum) {
        CreditCards creditCard = creditCardRepository
                .findByIdWithCustomerData(ccNum)
                .orElse(null);

        if (creditCard == null || creditCard.getCustomer() == null) {
            return errorResponse(false, "No se encontró cliente para la tarjeta: " + ccNum);
        }

        Customer customer = creditCard.getCustomer();
        Localization location = customer.getLocalization();
        Gender gender = customer.getGender();

        Integer customerAge = customer.getDob() != null
                ? Period.between(customer.getDob(), LocalDate.now()).getYears()
                : null;
        String customerLocation = location != null
                ? location.getCity() + ", " + location.getState()
                : null;

        return WhatIfResponseDto.builder()
                .customerFound(true)
                .customerName(customer.getFirstName() + " " + customer.getSurname())
                .customerLocation(customerLocation)
                .customerGender(gender != null ? gender.getGenderDescription() : null)
                .customerAge(customerAge)
                .build();
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private SavedPair saveTransactionAndPrediction(
            CreditCards creditCard,
            WhatIfRequestDto request,
            LocalDateTime transDateTime,
            String transactionId,
            FraudPredictionResponseDto apiResponse) {

        OperationalTransactions transaction = new OperationalTransactions();
        transaction.setCreditCard(creditCard);
        transaction.setTransNum(transactionId);
        transaction.setTransDateTime(transDateTime);
        transaction.setAmt(BigDecimal.valueOf(request.getAmt()));
        transaction.setMerchant(request.getMerchant());

        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            Category category = categoryRepository
                    .findByCategoryName(request.getCategory())
                    .orElse(null);
            transaction.setCategory(category);
        }

        transaction.setMerchLat(request.getMerchLat());
        transaction.setMerchLong(request.getMerchLong());
        transaction.setUnixTime(System.currentTimeMillis() / 1000);
        transaction.setIsFraudGroundTruth(null);

        OperationalTransactions savedTransaction = transactionRepository.save(transaction);
        FraudPredictions savedPrediction = predictionMapper.savePrediction(
                savedTransaction, apiResponse, FraudConstants.SCENARIO_INDIVIDUAL);

        return new SavedPair(savedTransaction, savedPrediction);
    }

    private static WhatIfResponseDto errorResponse(boolean customerFound, String message) {
        return WhatIfResponseDto.builder()
                .customerFound(customerFound)
                .error(message)
                .build();
    }
}
