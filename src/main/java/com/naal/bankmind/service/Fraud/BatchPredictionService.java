package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.entity.*;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para procesamiento de predicciones por lotes
 * 
 * OPTIMIZADO: Usa una sola llamada a la API para todo el lote
 * en lugar de una llamada por transacción.
 */
@Service
public class BatchPredictionService {

    private final TransactionRepository transactionRepository;
    private final FraudPredictionRepository fraudPredictionRepository;
    private final FraudApiClient fraudApiClient;

    private static final int MAX_BATCH_SIZE = 100;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public BatchPredictionService(
            TransactionRepository transactionRepository,
            FraudPredictionRepository fraudPredictionRepository,
            FraudApiClient fraudApiClient) {
        this.transactionRepository = transactionRepository;
        this.fraudPredictionRepository = fraudPredictionRepository;
        this.fraudApiClient = fraudApiClient;
    }

    /**
     * Obtiene el conteo de transacciones pendientes de análisis
     */
    public long getPendingCount() {
        return transactionRepository.countPendingPredictions();
    }

    /**
     * Obtiene lista de transacciones pendientes de análisis
     */
    @Transactional(readOnly = true)
    public List<PendingTransactionDto> getPendingTransactions(int limit) {
        int size = Math.min(limit, MAX_BATCH_SIZE);
        List<OperationalTransactions> pending = transactionRepository.findPendingPredictions(PageRequest.of(0, size));

        return pending.stream()
                .map(this::mapToPendingDto)
                .collect(Collectors.toList());
    }

    /**
     * Procesa un lote de transacciones por sus IDs
     * OPTIMIZADO: Una sola llamada HTTP a la API de Python
     */
    @Transactional
    public BatchResultDto processBatch(List<Long> transactionIds) {
        int size = Math.min(transactionIds.size(), MAX_BATCH_SIZE);
        List<Long> idsToProcess = transactionIds.subList(0, size);

        List<BatchResultDto.BatchItemResultDto> results = new ArrayList<>();
        int totalFrauds = 0;
        int totalLegitimate = 0;
        int totalErrors = 0;

        // 1. CARGAR TODAS LAS TRANSACCIONES CON DATOS DEL CLIENTE
        Map<Long, OperationalTransactions> transactionsMap = new HashMap<>();
        List<FraudPredictionRequestDto> apiRequests = new ArrayList<>();
        Map<String, Long> transNumToIdMap = new HashMap<>();

        for (Long transactionId : idsToProcess) {
            // Verificar si ya tiene predicción
            if (fraudPredictionRepository.existsByTransactionIdTransaction(transactionId)) {
                results.add(BatchResultDto.BatchItemResultDto.builder()
                        .idTransaction(transactionId)
                        .status("error")
                        .errorMessage("Ya tiene predicción")
                        .build());
                totalErrors++;
                continue;
            }

            // Obtener transacción con datos del cliente
            OperationalTransactions transaction = transactionRepository.findByIdWithCustomerData(transactionId)
                    .orElse(null);

            if (transaction == null) {
                results.add(BatchResultDto.BatchItemResultDto.builder()
                        .idTransaction(transactionId)
                        .status("error")
                        .errorMessage("Transacción no encontrada")
                        .build());
                totalErrors++;
                continue;
            }

            transactionsMap.put(transactionId, transaction);
            FraudPredictionRequestDto apiRequest = buildApiRequest(transaction);
            apiRequests.add(apiRequest);
            transNumToIdMap.put(transaction.getTransNum(), transactionId);
        }

        // 2. LLAMAR A LA API DE IA CON TODO EL LOTE (UNA SOLA LLAMADA HTTP)
        if (!apiRequests.isEmpty()) {
            try {
                BatchApiResponseDto batchResponse = fraudApiClient.predictFraudBatch(apiRequests);

                // 3. PROCESAR RESULTADOS Y GUARDAR PREDICCIONES
                for (FraudPredictionResponseDto apiResponse : batchResponse.getResults()) {
                    Long transactionId = transNumToIdMap.get(apiResponse.getTransactionId());
                    OperationalTransactions transaction = transactionsMap.get(transactionId);

                    if (transaction != null && apiResponse.getVeredicto() != null
                            && !"ERROR".equals(apiResponse.getVeredicto())) {
                        savePrediction(transaction, apiResponse);

                        boolean isFraud = "ALTO RIESGO".equals(apiResponse.getVeredicto());
                        results.add(BatchResultDto.BatchItemResultDto.builder()
                                .idTransaction(transactionId)
                                .transNum(transaction.getTransNum())
                                .amt(transaction.getAmt().doubleValue())
                                .veredicto(apiResponse.getVeredicto())
                                .score(apiResponse.getScoreFinal())
                                .status("success")
                                .build());

                        if (isFraud) {
                            totalFrauds++;
                        } else {
                            totalLegitimate++;
                        }
                    } else {
                        results.add(BatchResultDto.BatchItemResultDto.builder()
                                .idTransaction(transactionId)
                                .status("error")
                                .errorMessage(
                                        apiResponse.getError() != null ? apiResponse.getError() : "Error en predicción")
                                .build());
                        totalErrors++;
                    }
                }
            } catch (Exception e) {
                // Si falla la llamada batch, registrar error para todas las transacciones
                // pendientes
                for (Long id : transactionsMap.keySet()) {
                    results.add(BatchResultDto.BatchItemResultDto.builder()
                            .idTransaction(id)
                            .status("error")
                            .errorMessage("Error API: " + e.getMessage())
                            .build());
                    totalErrors++;
                }
            }
        }

        return BatchResultDto.builder()
                .totalProcessed(results.size())
                .totalFrauds(totalFrauds)
                .totalLegitimate(totalLegitimate)
                .totalErrors(totalErrors)
                .results(results)
                .build();
    }

    /**
     * Procesa automáticamente las siguientes N transacciones pendientes
     */
    @Transactional
    public BatchResultDto processNextBatch(int limit) {
        int size = Math.min(limit, MAX_BATCH_SIZE);
        List<Long> pendingIds = transactionRepository.findPendingPredictionIds(PageRequest.of(0, size));
        return processBatch(pendingIds);
    }

    // ================== MÉTODOS PRIVADOS ==================

    private PendingTransactionDto mapToPendingDto(OperationalTransactions t) {
        CreditCards cc = t.getCreditCard();
        Customer customer = cc != null ? cc.getCustomer() : null;

        String ccNumMasked = cc != null && cc.getCcNum() != null
                ? "****" + String.valueOf(cc.getCcNum())
                        .substring(Math.max(0, String.valueOf(cc.getCcNum()).length() - 4))
                : null;

        return PendingTransactionDto.builder()
                .idTransaction(t.getIdTransaction())
                .transNum(t.getTransNum())
                .transDateTime(t.getTransDateTime())
                .amt(t.getAmt() != null ? t.getAmt().doubleValue() : null)
                .category(t.getCategoryName())
                .merchant(t.getMerchant())
                .customerName(customer != null ? customer.getFirstName() + " " + customer.getSurname() : null)
                .ccNumMasked(ccNumMasked)
                .build();
    }

    private FraudPredictionRequestDto buildApiRequest(OperationalTransactions transaction) {
        CreditCards cc = transaction.getCreditCard();
        Customer customer = cc != null ? cc.getCustomer() : null;
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
                .gender(gender != null ? extractGenderCode(gender) : "M")
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

    private void savePrediction(OperationalTransactions transaction, FraudPredictionResponseDto response) {
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
        prediction.setDetectionScenario(2); // Procesamiento por lotes

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

        fraudPredictionRepository.save(prediction);
    }
}
