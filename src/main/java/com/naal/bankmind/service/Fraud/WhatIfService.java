package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.CreditCardRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Servicio para predicciones What-If (simulación)
 * 
 * Responsabilidad: Simular predicciones SIN guardar en BD
 * - Busca cliente por tarjeta
 * - Enriquece datos con información del cliente
 * - Llama a la API de IA
 * - NO persiste nada en la base de datos
 */
@Service
public class WhatIfService {

    private final CreditCardRepository creditCardRepository;
    private final FraudApiClient fraudApiClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public WhatIfService(
            CreditCardRepository creditCardRepository,
            FraudApiClient fraudApiClient) {
        this.creditCardRepository = creditCardRepository;
        this.fraudApiClient = fraudApiClient;
    }

    /**
     * Simula una predicción de fraude SIN guardar en BD
     */
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

            // 4. Construir request para API de IA
            FraudPredictionRequestDto apiRequest = FraudPredictionRequestDto.builder()
                    .transactionId("WHATIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
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

            // 5. Llamar a la API de IA (SIN persistir)
            FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(apiRequest);

            // 6. Construir respuesta enriquecida
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
                    .transactionId(apiResponse.getTransactionId())
                    .veredicto(apiResponse.getVeredicto())
                    .scoreFinal(apiResponse.getScoreFinal())
                    .detallesRiesgo(apiResponse.getDetallesRiesgo())
                    .datosAuditoria(apiResponse.getDatosAuditoria())
                    .recomendacion(apiResponse.getRecomendacion())
                    .build();

        } catch (Exception e) {
            return WhatIfResponseDto.builder()
                    .customerFound(false)
                    .error("Error al simular predicción: " + e.getMessage())
                    .build();
        }
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
