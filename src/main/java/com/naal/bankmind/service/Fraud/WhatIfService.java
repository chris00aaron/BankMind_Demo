package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.CategoryRepository;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para predicciones What-If (simulación)
 * 
 * Responsabilidad: Simular predicciones con opción de guardar en BD
 * - Busca cliente por tarjeta
 * - Enriquece datos con información del cliente
 * - Llama a la API de IA
 * - Opcionalmente persiste transacción y predicción en BD
 */
@Service
public class WhatIfService {

    private final CreditCardRepository creditCardRepository;
    private final FraudApiClient fraudApiClient;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudEmailService fraudEmailService;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public WhatIfService(
            CreditCardRepository creditCardRepository,
            FraudApiClient fraudApiClient,
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            FraudPredictionRepository fraudPredictionRepository,
            FraudEmailService fraudEmailService) {
        this.creditCardRepository = creditCardRepository;
        this.fraudApiClient = fraudApiClient;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.fraudEmailService = fraudEmailService;
    }

    /**
     * Simula una predicción de fraude, con opción de guardar en BD
     */
    @Transactional
    public WhatIfResponseDto simulatePrediction(WhatIfRequestDto request) {
        try {
            // 1. Buscar tarjeta y cliente
            CreditCards creditCard = creditCardRepository.findByIdWithCustomerData(request.getCcNum())
                    .orElse(null);

            if (creditCard == null || creditCard.getCustomer() == null) {
                return WhatIfResponseDto.builder()
                        .customerFound(false)
                        .error("No se encontró cliente para la tarjeta: " + request.getCcNum())
                        .build();
            }

            // VALIDACIÓN CRÍTICA: Verificar que la tarjeta esté activa
            if (creditCard.getIsActive() == null || !creditCard.getIsActive()) {
                return WhatIfResponseDto.builder()
                        .customerFound(true)
                        .error("TARJETA BLOQUEADA: Esta tarjeta ha sido bloqueada por seguridad. " +
                                "No se pueden procesar transacciones. " +
                                "Para más información, contacte a servicio al cliente: 1-800-BANKMIND")
                        .build();
            }

            Customer customer = creditCard.getCustomer();
            Localization location = customer.getLocalization();
            Gender gender = customer.getGender();

            // 2. Calcular edad
            Integer customerAge = null;
            if (customer.getDob() != null) {
                customerAge = Period.between(customer.getDob(), LocalDate.now()).getYears();
            }

            // 3. Construir datetime simulado con la hora especificada
            LocalDateTime simulatedDateTime = LocalDateTime.now()
                    .withHour(request.getHour() != null ? request.getHour() : LocalDateTime.now().getHour())
                    .withMinute(0)
                    .withSecond(0);

            // 4. Generar ID de transacción
            String transactionId = Boolean.TRUE.equals(request.getSaveToDB())
                    ? "TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                    : "WHATIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // 5. Construir request para API de IA
            FraudPredictionRequestDto apiRequest = FraudPredictionRequestDto.builder()
                    .transactionId(transactionId)
                    .idCliente(customer.getIdCustomer().toString())
                    .transDateTransTime(simulatedDateTime.format(DATE_TIME_FORMATTER))
                    .amt(request.getAmt())
                    .category(request.getCategory())
                    .gender(extractGenderCode(gender))
                    .job(customer.getJob() != null ? customer.getJob() : "Unknown")
                    .cityPop(location != null ? location.getCityPop() : 0)
                    .dob(customer.getDob() != null ? customer.getDob().format(DATE_FORMATTER) : "1990-01-01")
                    .lat(location != null ? location.getCustomerLat() : 0.0)
                    .lng(location != null ? location.getCustomerLong() : 0.0)
                    .merchLat(request.getMerchLat() != null ? request.getMerchLat() : 0.0)
                    .merchLong(request.getMerchLong() != null ? request.getMerchLong() : 0.0)
                    .build();

            // 6. Llamar a la API de IA
            FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(apiRequest);

            // 7. Si saveToDB es true, guardar transacción y predicción
            boolean savedToDB = false;
            OperationalTransactions savedTransaction = null;
            FraudPredictions savedPrediction = null;

            if (Boolean.TRUE.equals(request.getSaveToDB())) {
                if (request.getMerchant() == null || request.getMerchant().trim().isEmpty()) {
                    return WhatIfResponseDto.builder()
                            .customerFound(true)
                            .error("El campo 'merchant' es requerido para guardar en BD")
                            .build();
                }

                // Guardar transacción y predicción
                Object[] saved = saveTransactionAndPrediction(creditCard, request, simulatedDateTime, transactionId,
                        apiResponse);
                savedTransaction = (OperationalTransactions) saved[0];
                savedPrediction = (FraudPredictions) saved[1];
                savedToDB = true;

                // Decisión basada en veredicto de IA
                if ("ALTO RIESGO".equals(apiResponse.getVeredicto())) {
                    // Mantener PENDING y enviar email si el customer tiene email configurado
                    if (creditCard.getCustomer().getEmail() != null &&
                            !creditCard.getCustomer().getEmail().trim().isEmpty()) {
                        fraudEmailService.sendFraudAlert(savedTransaction, savedPrediction);
                    }
                } else {
                    // Si es BAJO RIESGO, aprobar automáticamente
                    savedTransaction.setStatus("APPROVED");
                    transactionRepository.save(savedTransaction);
                }
            }

            // 8. Construir respuesta enriquecida
            String customerLocation = null;
            if (location != null) {
                customerLocation = location.getCity() + ", " + location.getState();
            }

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
            return WhatIfResponseDto.builder()
                    .customerFound(false)
                    .error("Error al simular predicción: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Guarda la transacción y la predicción en BD
     * 
     * @return Object[] con [0] = OperationalTransactions, [1] = FraudPredictions
     */
    private Object[] saveTransactionAndPrediction(
            CreditCards creditCard,
            WhatIfRequestDto request,
            LocalDateTime transDateTime,
            String transactionId,
            FraudPredictionResponseDto apiResponse) {

        // 1. Crear transacción
        OperationalTransactions transaction = new OperationalTransactions();
        transaction.setCreditCard(creditCard);
        transaction.setTransNum(transactionId);
        transaction.setTransDateTime(transDateTime);
        transaction.setAmt(BigDecimal.valueOf(request.getAmt()));
        transaction.setMerchant(request.getMerchant());

        // Buscar categoría por nombre
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            Category category = categoryRepository.findByCategoryName(request.getCategory())
                    .orElse(null);
            transaction.setCategory(category);
        }

        transaction.setMerchLat(request.getMerchLat());
        transaction.setMerchLong(request.getMerchLong());
        transaction.setUnixTime(System.currentTimeMillis() / 1000);
        transaction.setIsFraudGroundTruth(null);

        // 2. Guardar transacción
        OperationalTransactions savedTransaction = transactionRepository.save(transaction);

        // 3. Crear predicción
        FraudPredictions prediction = new FraudPredictions();
        prediction.setTransaction(savedTransaction);
        prediction.setVeredicto(apiResponse.getVeredicto());
        prediction.setPredictionDate(LocalDateTime.now());

        Map<String, Object> auditData = apiResponse.getDatosAuditoria();
        if (auditData != null) {
            if (auditData.get("xgboost_score") != null) {
                prediction.setXgboostScore(((Number) auditData.get("xgboost_score")).floatValue());
            }
            if (auditData.get("iforest_score") != null) {
                prediction.setIfforestScore(((Number) auditData.get("iforest_score")).floatValue());
            }
        }

        prediction.setFinalDecision("ALTO RIESGO".equals(apiResponse.getVeredicto()) ? 1 : 0);
        prediction.setDetectionScenario(1);

        // 4. Guardar detalles de riesgo
        if (apiResponse.getDetallesRiesgo() != null) {
            for (RiskFactorDto riskFactor : apiResponse.getDetallesRiesgo()) {
                PredictionDetails detail = new PredictionDetails();
                detail.setFraudPrediction(prediction);
                detail.setFeatureName(riskFactor.getFeatureName());
                detail.setFeatureValue(riskFactor.getFeatureValue());
                detail.setShapValue(riskFactor.getShapValue());
                detail.setRiskDescription(riskFactor.getRiskDescription());
                detail.setImpactDirection(riskFactor.getImpactDirection());
                prediction.getDetails().add(detail);
            }
        }

        FraudPredictions savedPrediction = fraudPredictionRepository.save(prediction);

        return new Object[] { savedTransaction, savedPrediction };
    }

    /**
     * Buscar información del cliente por número de tarjeta (para mostrar antes de
     * simular)
     */
    public WhatIfResponseDto lookupCustomer(Long ccNum) {
        CreditCards creditCard = creditCardRepository.findByIdWithCustomerData(ccNum)
                .orElse(null);

        if (creditCard == null || creditCard.getCustomer() == null) {
            return WhatIfResponseDto.builder()
                    .customerFound(false)
                    .error("No se encontró cliente para la tarjeta: " + ccNum)
                    .build();
        }

        Customer customer = creditCard.getCustomer();
        Localization location = customer.getLocalization();
        Gender gender = customer.getGender();

        Integer customerAge = null;
        if (customer.getDob() != null) {
            customerAge = Period.between(customer.getDob(), LocalDate.now()).getYears();
        }

        String customerLocation = null;
        if (location != null) {
            customerLocation = location.getCity() + ", " + location.getState();
        }

        return WhatIfResponseDto.builder()
                .customerFound(true)
                .customerName(customer.getFirstName() + " " + customer.getSurname())
                .customerLocation(customerLocation)
                .customerGender(gender != null ? gender.getGenderDescription() : null)
                .customerAge(customerAge)
                .build();
    }

    private String extractGenderCode(Gender gender) {
        if (gender == null)
            return "M";
        String genderDesc = gender.getGenderDescription();
        if (genderDesc != null) {
            genderDesc = genderDesc.toLowerCase();
            if (genderDesc.contains("female") || genderDesc.contains("femenino") || genderDesc.startsWith("f")) {
                return "F";
            }
        }
        return "M";
    }
}
