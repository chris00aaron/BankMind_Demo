package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import com.naal.bankmind.dto.Fraud.RiskFactorDto;
import com.naal.bankmind.entity.Customer;
import com.naal.bankmind.entity.Gender;
import com.naal.bankmind.entity.Localization;
import com.naal.bankmind.entity.Fraud.FraudPredictions;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.entity.Fraud.PredictionDetails;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper centralizado del módulo de Fraude.
 *
 * Responsabilidad única: transformación de datos entre entidades,
 * DTOs y la API de IA. Elimina la duplicación que existía en
 * FraudPredictionService, BatchPredictionService y WhatIfService.
 *
 * Principios aplicados:
 * - SRP: solo transforma, no orquesta negocio
 * - DRY: buildApiRequest, extractGenderCode y savePrediction
 * existían en tres copias idénticas — ahora viven aquí
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionMapper {

    private final FraudPredictionRepository fraudPredictionRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ──────────────────────────────────────────────────────────────────────────
    // REQUEST BUILDER
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Construye el DTO de request para la API de IA a partir de una entidad
     * de transacción ya cargada con sus relaciones (creditCard → customer → ...).
     */
    public FraudPredictionRequestDto buildApiRequest(OperationalTransactions transaction) {
        Customer customer = transaction.getCreditCard() != null
                ? transaction.getCreditCard().getCustomer()
                : null;
        Localization location = customer != null ? customer.getLocalization() : null;
        Gender gender = customer != null ? customer.getGender() : null;

        return FraudPredictionRequestDto.builder()
                .transactionId(transaction.getTransNum())
                .idCliente(customer != null ? customer.getIdCustomer().toString() : "0")
                .transDateTransTime(transaction.getTransDateTime() != null
                        ? transaction.getTransDateTime().format(DATE_TIME_FORMATTER)
                        : LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .amt(transaction.getAmt() != null ? transaction.getAmt().doubleValue() : 0.0)
                .category(transaction.getCategoryName())
                .gender(extractGenderCode(gender))
                .job(customer != null && customer.getJob() != null ? customer.getJob() : "Unknown")
                .cityPop(location != null ? location.getCityPop() : 0)
                .dob(customer != null && customer.getDob() != null
                        ? customer.getDob().format(DATE_FORMATTER)
                        : "1990-01-01")
                .lat(location != null ? location.getCustomerLat() : 0.0)
                .lng(location != null ? location.getCustomerLong() : 0.0)
                .merchLat(transaction.getMerchLat() != null ? transaction.getMerchLat() : 0.0)
                .merchLong(transaction.getMerchLong() != null ? transaction.getMerchLong() : 0.0)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PREDICTION PERSISTENCE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Persiste una predicción y sus detalles SHAP en la base de datos.
     *
     * @param transaction       Entidad de transacción ya guardada
     * @param apiResponse       Respuesta de la API de IA
     * @param detectionScenario {@link FraudConstants#SCENARIO_INDIVIDUAL} o
     *                          {@link FraudConstants#SCENARIO_BATCH}
     * @return entidad persistida
     */
    public FraudPredictions savePrediction(
            OperationalTransactions transaction,
            FraudPredictionResponseDto apiResponse,
            int detectionScenario) {

        FraudPredictions prediction = new FraudPredictions();
        prediction.setTransaction(transaction);
        prediction.setVeredicto(apiResponse.getVeredicto());
        prediction.setPredictionDate(LocalDateTime.now());
        prediction.setFinalDecision(
                FraudConstants.VEREDICTO_ALTO_RIESGO.equals(apiResponse.getVeredicto()) ? 1 : 0);
        prediction.setDetectionScenario(detectionScenario);

        Map<String, Object> auditData = apiResponse.getDatosAuditoria();
        if (auditData != null) {
            if (auditData.get("xgboost_score") instanceof Number n) {
                prediction.setXgboostScore(n.floatValue());
            }
            if (auditData.get("iforest_score") instanceof Number n) {
                prediction.setIfforestScore(n.floatValue());
            }
        }

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

        FraudPredictions saved = fraudPredictionRepository.save(prediction);
        log.debug("Predicción persistida: id={}, veredicto={}, scenario={}",
                saved.getIdFraudPrediction(), saved.getVeredicto(), detectionScenario);
        return saved;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GENDER HELPER
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Extrae el código de género ('F' / 'M') desde la entidad {@link Gender}.
     * Soporta: "female", "femenino", "f" → 'F'; cualquier otro → 'M'.
     */
    public String extractGenderCode(Gender gender) {
        if (gender == null)
            return FraudConstants.GENDER_MALE_CODE;
        String desc = gender.getGenderDescription();
        if (desc != null) {
            desc = desc.toLowerCase();
            if (desc.contains("female") || desc.contains("femenino") || desc.startsWith("f")) {
                return FraudConstants.GENDER_FEMALE_CODE;
            }
        }
        return FraudConstants.GENDER_MALE_CODE;
    }
}
