package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.FraudPredictionRequestDto;
import com.naal.bankmind.dto.Fraud.FraudPredictionResponseDto;
import com.naal.bankmind.dto.Fraud.RiskFactorDto;
import com.naal.bankmind.entity.Fraud.FraudPredictions;
import com.naal.bankmind.entity.Fraud.OperationalTransactions;
import com.naal.bankmind.repository.Fraud.FraudPredictionRepository;
import com.naal.bankmind.repository.Fraud.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio especializado en PREDICCIÓN de fraude.
 *
 * Responsabilidad única: orquestación del flujo de predicción individual
 * (obtener transacción → llamar API de IA → persistir resultado).
 * La transformación de datos está delegada a {@link PredictionMapper}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudPredictionService {

        private final TransactionRepository transactionRepository;
        private final FraudPredictionRepository fraudPredictionRepository;
        private final FraudApiClient fraudApiClient;
        private final PredictionMapper predictionMapper;

        private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        /**
         * Realiza predicción de fraude para una transacción existente.
         */
        @Transactional
        public FraudPredictionResponseDto predictTransaction(Long transactionId) {
                if (fraudPredictionRepository.existsByTransactionIdTransaction(transactionId)) {
                        throw new IllegalStateException(
                                        "Ya existe una predicción para la transacción ID: " + transactionId);
                }

                OperationalTransactions transaction = transactionRepository
                                .findByIdWithCustomerData(transactionId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Transacción no encontrada con ID: " + transactionId));

                FraudPredictionRequestDto request = predictionMapper.buildApiRequest(transaction);
                FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(request);

                predictionMapper.savePrediction(transaction, apiResponse, FraudConstants.SCENARIO_INDIVIDUAL);

                log.info("Predicción individual completada: tx={}, veredicto={}",
                                transactionId, apiResponse.getVeredicto());
                return apiResponse;
        }

        /**
         * Predicción directa con request ya construido (usado por TransactionService).
         */
        @Transactional
        public FraudPredictionResponseDto predictTransactionDirect(
                        FraudPredictionRequestDto request,
                        OperationalTransactions transaction) {
                FraudPredictionResponseDto apiResponse = fraudApiClient.predictFraud(request);
                predictionMapper.savePrediction(transaction, apiResponse, FraudConstants.SCENARIO_INDIVIDUAL);
                return apiResponse;
        }

        /**
         * Obtiene predicción existente por ID de transacción.
         */
        public FraudPredictionResponseDto getPredictionByTransactionId(Long transactionId) {
                FraudPredictions prediction = fraudPredictionRepository
                                .findByTransactionIdTransaction(transactionId)
                                .orElseThrow(
                                                () -> new RuntimeException(
                                                                "No existe predicción para la transacción ID: "
                                                                                + transactionId));
                return mapToResponseDto(prediction);
        }

        /**
         * Lista todas las predicciones.
         */
        public List<FraudPredictionResponseDto> getAllPredictions() {
                return fraudPredictionRepository.findAll().stream()
                                .map(this::mapToResponseDto)
                                .collect(Collectors.toList());
        }

        /**
         * Lista predicciones filtradas por veredicto.
         */
        public List<FraudPredictionResponseDto> getPredictionsByVeredicto(String veredicto) {
                return fraudPredictionRepository.findByVeredicto(veredicto).stream()
                                .map(this::mapToResponseDto)
                                .collect(Collectors.toList());
        }

        /**
         * Verifica si la API de fraude está disponible.
         */
        public boolean isApiAvailable() {
                return fraudApiClient.isApiAvailable();
        }

        // ─── Mapeo de respuesta ───────────────────────────────────────────────────

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

                String recomendacion = FraudConstants.VEREDICTO_ALTO_RIESGO.equals(prediction.getVeredicto())
                                ? FraudConstants.RECOMENDACION_BLOQUEAR
                                : FraudConstants.RECOMENDACION_APROBAR;

                return FraudPredictionResponseDto.builder()
                                .transactionId(prediction.getTransaction().getTransNum())
                                .veredicto(prediction.getVeredicto())
                                .scoreFinal(prediction.getXgboostScore())
                                .detallesRiesgo(riskFactors)
                                .datosAuditoria(Map.of(
                                                "xgboost_score",
                                                prediction.getXgboostScore() != null ? prediction.getXgboostScore()
                                                                : 0.0,
                                                "iforest_score",
                                                prediction.getIfforestScore() != null ? prediction.getIfforestScore()
                                                                : 0.0,
                                                "prediction_id", prediction.getIdFraudPrediction()))
                                .recomendacion(recomendacion)
                                .build();
        }
}
