package com.naal.bankmind.service.Fraud;

import com.naal.bankmind.dto.Fraud.ChampionModelDto;
import com.naal.bankmind.dto.Fraud.DriftModelOptionDto;
import com.naal.bankmind.dto.Fraud.FeatureDriftDto;
import com.naal.bankmind.dto.Fraud.TrainingAuditDto;
import com.naal.bankmind.entity.Fraud.ModelFeatureDrift;
import com.naal.bankmind.entity.Fraud.SelfTrainingAuditFraud;
import com.naal.bankmind.repository.Fraud.ModelFeatureDriftRepository;
import com.naal.bankmind.repository.Fraud.SelfTrainingAuditFraudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de Monitoreo del Modelo de Fraude.
 *
 * Expone los datos reales de:
 * - Modelo CHAMPION activo (fraud_models)
 * - Historial de entrenamientos (self_training_audit_fraud)
 * - Evolución del PSI por feature (model_feature_drift)
 * - Disparador manual de entrenamiento (→ Python Training API)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudMonitoringService {

    private final SelfTrainingAuditFraudRepository auditRepository;
    private final ModelFeatureDriftRepository featureDriftRepository;
    private final JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Value("${fraud.scheduler.training-url:http://localhost:8001/fraude/train}")
    private String trainingApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===========================================================================
    // CHAMPION MODEL
    // ===========================================================================

    /**
     * Retorna el modelo CHAMPION activo junto con las métricas del último
     * entrenamiento que lo llevó a producción.
     */
    public ChampionModelDto getChampionModel() {
        // Leer el champion de fraud_models mediante JdbcTemplate (JSONB en PostgreSQL)
        String sql = """
                SELECT fm.id_model, fm.model_version, fm.promotion_status, fm.is_active,
                       fm.created_at,
                       (fm.model_config->>'optimal_threshold')::numeric AS threshold
                FROM fraud_models fm
                WHERE fm.is_active = true
                ORDER BY fm.id_model DESC
                LIMIT 1
                """;

        List<ChampionModelDto> results = jdbcTemplate.query(sql, (rs, rowNum) -> ChampionModelDto.builder()
                .idModel(rs.getLong("id_model"))
                .modelVersion(rs.getString("model_version"))
                .promotionStatus(rs.getString("promotion_status"))
                .isActive(rs.getBoolean("is_active"))
                .algorithm("XGBoost + IsolationForest") // Constante del diseño
                .createdAt(rs.getObject("created_at", LocalDateTime.class))
                .threshold(rs.getBigDecimal("threshold"))
                .build());

        if (results.isEmpty()) {
            return null;
        }

        ChampionModelDto champion = results.get(0);

        // Enriquecer con métricas del último ciclo de entrenamiento PROMOTED
        SelfTrainingAuditFraud lastAudit = auditRepository
                .findTopByIsSuccessTrueOrderByEndTrainingDesc();

        if (lastAudit != null) {
            champion.setAccuracy(lastAudit.getAccuracy());
            champion.setPrecisionScore(lastAudit.getPrecisionScore());
            champion.setRecallScore(lastAudit.getRecallScore());
            champion.setF1Score(lastAudit.getF1Score());
            champion.setAucRoc(lastAudit.getAucRoc());
            champion.setPromotedAt(lastAudit.getEndTraining());
        }

        return champion;
    }

    // ===========================================================================
    // TRAINING HISTORY
    // ===========================================================================

    /**
     * Retorna el historial de los últimos N ciclos de entrenamiento,
     * con métricas tanto del challenger como del champion de referencia.
     */
    public List<TrainingAuditDto> getTrainingHistory(int limit) {
        List<SelfTrainingAuditFraud> audits = auditRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getEndTraining() == null)
                        return 1;
                    if (b.getEndTraining() == null)
                        return -1;
                    return b.getEndTraining().compareTo(a.getEndTraining());
                })
                .limit(limit)
                .collect(Collectors.toList());

        return audits.stream()
                .map(this::toTrainingAuditDto)
                .collect(Collectors.toList());
    }

    private TrainingAuditDto toTrainingAuditDto(SelfTrainingAuditFraud a) {
        Integer duration = null;
        if (a.getStartTraining() != null && a.getEndTraining() != null) {
            duration = (int) ChronoUnit.SECONDS.between(a.getStartTraining(), a.getEndTraining());
        }

        // Buscar el dataset asociado a este audit mediante JOIN por id_audit
        TrainingAuditDto.TrainingAuditDtoBuilder builder = TrainingAuditDto.builder()
                .idAudit(a.getIdAudit())
                .idModel(a.getModel() != null ? a.getModel().getIdModel() : null)
                .startTraining(a.getStartTraining())
                .endTraining(a.getEndTraining())
                .trainingDurationSeconds(a.getTrainingDurationSeconds() != null
                        ? a.getTrainingDurationSeconds()
                        : duration)
                .accuracy(a.getAccuracy())
                .precisionScore(a.getPrecisionScore())
                .recallScore(a.getRecallScore())
                .f1Score(a.getF1Score())
                .aucRoc(a.getAucRoc())
                .optimalThreshold(a.getOptimalThreshold())
                .championF1Score(a.getChampionF1Score())
                .championRecall(a.getChampionRecall())
                .championAucRoc(a.getChampionAucRoc())
                .optunaBestF1(a.getOptunaBestF1())
                .optunaBestParams(a.getOptunaBestParams())
                .promotionStatus(a.getPromotionStatus())
                .promotionReason(a.getPromotionReason())
                .triggeredBy(a.getTriggeredBy())
                .isSuccess(a.getIsSuccess())
                .errorMessage(a.getErrorMessage());

        // Enriquecer con datos del dataset (JOIN a dataset_fraud_prediction)
        try {
            String datasetSql = """
                    SELECT d.id_dataset, d.start_date, d.end_date,
                           d.total_samples, d.count_train, d.count_test,
                           d.fraud_ratio, d.undersampling_ratio
                    FROM self_training_audit_fraud a
                    JOIN dataset_fraud_prediction d ON d.id_dataset = a.id_dataset
                    WHERE a.id_audit = ?
                    """;

            List<TrainingAuditDto> datasets = jdbcTemplate.query(
                    datasetSql,
                    (rs, rowNum) -> TrainingAuditDto.builder()
                            .datasetId(rs.getLong("id_dataset"))
                            .datasetStartDate(rs.getObject("start_date", LocalDateTime.class))
                            .datasetEndDate(rs.getObject("end_date", LocalDateTime.class))
                            .datasetTotalSamples(rs.getObject("total_samples", Integer.class))
                            .datasetCountTrain(rs.getObject("count_train", Integer.class))
                            .datasetCountTest(rs.getObject("count_test", Integer.class))
                            .datasetFraudRatio(rs.getBigDecimal("fraud_ratio"))
                            .datasetUndersamplingRatio(rs.getObject("undersampling_ratio", Integer.class))
                            .build(),
                    a.getIdAudit());

            if (!datasets.isEmpty()) {
                TrainingAuditDto ds = datasets.get(0);
                builder.datasetId(ds.getDatasetId())
                        .datasetStartDate(ds.getDatasetStartDate())
                        .datasetEndDate(ds.getDatasetEndDate())
                        .datasetTotalSamples(ds.getDatasetTotalSamples())
                        .datasetCountTrain(ds.getDatasetCountTrain())
                        .datasetCountTest(ds.getDatasetCountTest())
                        .datasetFraudRatio(ds.getDatasetFraudRatio())
                        .datasetUndersamplingRatio(ds.getDatasetUndersamplingRatio());
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener dataset info para audit {}: {}", a.getIdAudit(), e.getMessage());
        }

        return builder.build();
    }

    // ===========================================================================
    // PSI DRIFT HISTORY
    // ===========================================================================

    /**
     * Retorna el historial de PSI de todos los features del champion activo.
     * Usado para construir la gráfica de líneas "Evolución del PSI".
     */
    /**
     * Retorna el historial de PSI para la gráfica de líneas.
     *
     * - modelId == null → modo cross-model: todos los modelos, línea continua.
     * Este es el modo por defecto del dashboard (nivel MLOps senior).
     * - modelId != null → filtrado por ese modelo específico (auditoría).
     *
     * @param daysBack cuántos días hacia atrás consultar
     * @param modelId  null para cross-model, o id específico de modelo
     */
    public List<FeatureDriftDto> getDriftHistory(int daysBack, Long modelId) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysBack);

        List<ModelFeatureDrift> records = (modelId == null)
                ? featureDriftRepository.findAllSince(since)
                : featureDriftRepository.findRecentByModel(modelId, since);

        return records.stream()
                .map(d -> FeatureDriftDto.builder()
                        .idDrift(d.getIdDrift())
                        .idModel(d.getModel() != null ? d.getModel().getIdModel() : null)
                        .featureName(d.getFeatureName())
                        .psiValue(d.getPsiValue())
                        .driftCategory(d.getDriftCategory())
                        .measuredAt(d.getMeasuredAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Retorna el último PSI medido para cada feature (vista rápida del estado
     * actual).
     */
    /**
     * Último PSI medido por feature.
     *
     * - modelId == null → cross-model: el PSI más reciente de CADA feature,
     * sin importar en qué sesión de modelo fue medido.
     * - modelId != null → solo registros de ese modelo.
     */
    public List<FeatureDriftDto> getLatestDriftPerFeature(Long modelId) {
        List<ModelFeatureDrift> records = (modelId == null)
                ? featureDriftRepository.findLatestPerFeatureGlobal()
                : featureDriftRepository.findLatestPerFeatureByModel(modelId);

        return records.stream().map(d -> FeatureDriftDto.builder()
                .idDrift(d.getIdDrift())
                .idModel(d.getModel() != null ? d.getModel().getIdModel() : null)
                .featureName(d.getFeatureName())
                .psiValue(d.getPsiValue())
                .driftCategory(d.getDriftCategory())
                .measuredAt(d.getMeasuredAt())
                .build())
                .collect(Collectors.toList());
    }

    private Long getActiveChampionId() {
        String sql = "SELECT id_model FROM fraud_models WHERE is_active = true ORDER BY id_model DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    // ===========================================================================
    // MODEL OPTIONS (curated list for PSI chart selector)
    // ===========================================================================

    /**
     * Retorna la lista curada de modelos para el selector del gráfico de PSI.
     *
     * Reglas de curado (nivel MLOps):
     * 1. Champion activo siempre primero (is_active = true).
     * 2. Solo modelos con promotion_status = 'PROMOTED'.
     * 3. Máximo MAX_PROMOTED_OPTIONS modelos adicionales (excluyendo champion).
     *
     * Así el selector escala correctamente aunque existan cientos de versiones.
     */
    private static final int MAX_PROMOTED_OPTIONS = 5;

    public List<DriftModelOptionDto> getDriftModelOptions() {
        // Los valores reales de promotion_status en fraud_models (definidos en
        // model_registry.py):
        // 'CHAMPION' → modelo activo actualmente en producción
        // 'RETIRED' → champion anterior, desplazado por uno nuevo
        // 'CHALLENGER' → entrenado pero nunca llegó a producción (excluido)
        String sql = """
                SELECT id_model, model_version, is_active, created_at
                FROM fraud_models
                WHERE promotion_status IN ('CHAMPION', 'RETIRED')
                ORDER BY is_active DESC, id_model DESC
                LIMIT :limit
                """;

        // +1 para garantizar que el champion entra aunque ya cuente en el límite
        int limit = MAX_PROMOTED_OPTIONS + 1;

        return jdbcTemplate.query(
                sql.replace(":limit", String.valueOf(limit)),
                (rs, rowNum) -> DriftModelOptionDto.builder()
                        .idModel(rs.getLong("id_model"))
                        .modelVersion(rs.getString("model_version"))
                        .isChampion(rs.getBoolean("is_active"))
                        .createdAt(rs.getObject("created_at", java.time.LocalDateTime.class))
                        .build());
    }

    // ===========================================================================
    // MANUAL TRAINING TRIGGER
    // ===========================================================================

    /**
     * Dispara un ciclo de entrenamiento manual llamando directamente a la Training
     * API.
     * El frontend pasa start_date y end_date para el rango de datos.
     */
    public Map<String, Object> triggerManualTraining(String startDate, String endDate) {
        log.info("🖐️ [MANUAL] Entrenamiento manual solicitado: {} → {}", startDate, endDate);

        Map<String, Object> trainingRequest = new HashMap<>();
        trainingRequest.put("start_date", startDate);
        trainingRequest.put("end_date", endDate);
        trainingRequest.put("optuna_trials", 20);
        trainingRequest.put("undersampling_ratio", 4.0);
        trainingRequest.put("triggered_by", "manual-frontend");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(trainingRequest, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    trainingApiUrl, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("✅ [MANUAL] Entrenamiento completado. Status: {}",
                        response.getBody().get("promotion_status"));
                return response.getBody();
            } else {
                log.warn("⚠️ [MANUAL] Training API respondió con: {}", response.getStatusCode());
                return Map.of("error", "Training API respondió con " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ [MANUAL] Error llamando Training API: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo conectar con la Training API: " + e.getMessage());
        }
    }
}
