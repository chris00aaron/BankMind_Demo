package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado en PREDICCIÓN de fraude
 * Responsabilidad: Llamar API de IA, persistir predicciones
 * 
 * Principio SOLID: Single Responsibility
 */
@Service
public class FraudPredictionService {

    private final TransactionRepository transactionRepository;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudApiClient fraudApiClient;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FraudPredictionService(
            TransactionRepository transactionRepository,
            FraudPredictionRepository fraudPredictionRepository,
            FraudApiClient fraudApiClient) {
        this.transactionRepository = transactionRepository;
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.fraudApiClient = fraudApiClient;
    }

    /**
     * Realiza predicción de fraude para una transacción existente
     */
    @Transactional
    public FraudPredictionResponseDto predictTransaction(Long transactionId) {
        // 1. Verificar si ya existe predicción
        if (fraudPredictionRepository.existsByTransactionIdTransaction(transactionId)) {
            throw new IllegalStateException("Ya existe una predicción para la transacción ID: " + transactionId);
        }

        // 2. Obtener transacción con datos del cliente
        OperationalTransactions transaction = transactionRepository.findByIdWithCustomerData(transactionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transactionId));

        // 3. Mapear a DTO de request
        FraudPredictionRequestDto request = mapToApiRequest(transaction);

        // 4. Llamar a la API de Fraude
        FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(request);

        // 5. Persistir resultado
        savePrediction(transaction, apiResponse);

        return apiResponse;
    }

    /**
     * Predicción directa con request ya construido (usado por TransactionService)
     */
    @Transactional
    public FraudPredictionResponseDto predictTransactionDirect(
            FraudPredictionRequestDto request,
            OperationalTransactions transaction) {
        FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(request);
        savePrediction(transaction, apiResponse);
        return apiResponse;
    }

    /**
     * Obtiene predicción existente por ID de transacción
     */
    public FraudPredictionResponseDto getPredictionByTransactionId(Long transactionId) {
        FraudPredictions prediction = fraudPredictionRepository.findByTransactionIdTransaction(transactionId)
                .orElseThrow(
                        () -> new RuntimeException("No existe predicción para la transacción ID: " + transactionId));
        return mapToResponseDto(prediction);
    }

    /**
     * Lista todas las predicciones
     */
    public List<FraudPredictionResponseDto> getAllPredictions() {
        return fraudPredictionRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Lista predicciones filtradas por veredicto
     */
    public List<FraudPredictionResponseDto> getPredictionsByVeredicto(String veredicto) {
        return fraudPredictionRepository.findByVeredicto(veredicto).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Verifica si la API de fraude está disponible
     */
    public boolean isApiAvailable() {
        return fraudApiClient.isApiAvailable();
    }

    // ================== MÉTODOS PRIVADOS ==================

    private FraudPredictionRequestDto mapToApiRequest(OperationalTransactions transaction) {
        Customer customer = transaction.getCreditCard().getCustomer();
        Localization location = customer.getLocalization();
        Gender gender = customer.getGender();

        return FraudPredictionRequestDto.builder()
                .transactionId(transaction.getTransNum())
                .idCliente(customer.getIdCustomer().toString())
                .transDateTransTime(transaction.getTransDateTime().format(DATE_TIME_FORMATTER))
                .amt(transaction.getAmt().doubleValue())
                .category(transaction.getCategoryName())
                .gender(gender != null ? extractGenderCode(gender) : "M")
                .job(customer.getJob())
                .cityPop(location != null ? location.getCityPop() : 0)
                .dob(customer.getDob() != null ? customer.getDob().format(DATE_FORMATTER) : "1990-01-01")
                .lat(location != null ? location.getCustomerLat() : 0.0)
                .lng(location != null ? location.getCustomerLong() : 0.0)
                .merchLat(transaction.getMerchLat() != null ? transaction.getMerchLat() : 0.0)
                .merchLong(transaction.getMerchLong() != null ? transaction.getMerchLong() : 0.0)
                .build();
    }

    private String extractGenderCode(Gender gender) {
        String genderDesc = gender.getGenderDescription();
        if (genderDesc != null) {
            genderDesc = genderDesc.toLowerCase();
            if (genderDesc.contains("female") || genderDesc.contains("femenino") || genderDesc.startsWith("f")) {
                return "F";
            }
        }
        return "M";
    }

    @Transactional
    protected FraudPredictions savePrediction(OperationalTransactions transaction,
            FraudPredictionResponseDto response) {
        FraudPredictions prediction = new FraudPredictions();
        prediction.setTransaction(transaction);
        prediction.setVeredicto(response.getVeredicto());
        prediction.setPredictionDate(LocalDateTime.now());

        Map<String, Object> auditData = response.getDatosAuditoria();
        if (auditData != null) {
            if (auditData.get("xgboost_score") != null) {
                prediction.setXgboostScore(((Number) auditData.get("xgboost_score")).floatValue());
            }
            if (auditData.get("iforest_score") != null) {
                prediction.setIfforestScore(((Number) auditData.get("iforest_score")).floatValue());
            }
        }

        prediction.setFinalDecision("ALTO RIESGO".equals(response.getVeredicto()) ? 1 : 0);
        prediction.setDetectionScenario(1);

        if (response.getDetallesRiesgo() != null) {
            for (RiskFactorDto riskFactor : response.getDetallesRiesgo()) {
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

        return fraudPredictionRepository.save(prediction);
    }

    private FraudPredictionResponseDto mapToResponseDto(FraudPredictions prediction) {
        List<RiskFactorDto> riskFactors = prediction.getDetails().stream()
                .map(d -> RiskFactorDto.builder()
                        .featureName(d.getFeatureName())
                        .featureValue(d.getFeatureValue())
                        .shapValue(d.getShapValue())
                        .riskDescription(d.getRiskDescription())
                        .impactDirection(d.getImpactDirection())
                        .build())
                .collect(Collectors.toList());

        return FraudPredictionResponseDto.builder()
                .transactionId(prediction.getTransaction().getTransNum())
                .veredicto(prediction.getVeredicto())
                .scoreFinal(prediction.getXgboostScore())
                .detallesRiesgo(riskFactors)
                .datosAuditoria(Map.of(
                        "xgboost_score", prediction.getXgboostScore() != null ? prediction.getXgboostScore() : 0.0,
                        "iforest_score", prediction.getIfforestScore() != null ? prediction.getIfforestScore() : 0.0,
                        "prediction_id", prediction.getIdFraudPrediction()))
                .recomendacion("ALTO RIESGO".equals(prediction.getVeredicto()) ? "Bloquear y Notificar" : "Aprobar")
                .build();
    }
}
