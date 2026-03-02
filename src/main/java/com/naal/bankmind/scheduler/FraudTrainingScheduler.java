package com.naal.bankmind.scheduler;

import com.naal.bankmind.entity.Fraud.SelfTrainingAuditFraud;
import com.naal.bankmind.repository.Fraud.ModelFeatureDriftRepository;
import com.naal.bankmind.repository.Fraud.SelfTrainingAuditFraudRepository;
import com.naal.bankmind.service.Fraud.FraudConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduler de autoentrenamiento y sensores de drift para el módulo de Fraude.
 *
 * Dos tareas programadas:
 * 1. {@link #runWeeklyTraining()} — Lunes 02:00 AM: entrenamiento semanal.
 * 2. {@link #checkDriftSensors()} — Diario 06:00 AM: sensor PSI + Recall.
 *
 * Mejoras vs. versión original:
 * - Constructor injection en lugar de @Autowired (testeable, SOLID).
 * - URLs leídas de application.properties vía @Value (sin hardcoding).
 * - RestTemplate inyectado con timeout configurado.
 * - Código muerto eliminado (bucle de ModelFeatureDrift comentado).
 * - Magic strings reemplazadas por FraudConstants.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FraudTrainingScheduler {

    private final SelfTrainingAuditFraudRepository auditRepository;
    private final ModelFeatureDriftRepository featureDriftRepository;
    private final RestTemplate schedulerRestTemplate; // Bean con timeout configurado

    @Value("${fraud.scheduler.training-url}")
    private String trainingApiUrl;

    @Value("${fraud.scheduler.drift-url}")
    private String driftApiUrl;

    @Value("${fraud.scheduler.reload-url}")
    private String predictionApiReloadUrl;

    // Umbrales de los sensores de drift
    private static final double PSI_THRESHOLD_CRITICAL = 0.25;
    private static final double RECALL_THRESHOLD_REACTIVE = 0.90;
    private static final List<String> CRITICAL_FEATURES = List.of("amt", "city_pop", "age");

    // ─────────────────────────────────────────────────────────────────────────
    // TAREA 1: Entrenamiento semanal — Lunes a las 2:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 2 * * MON")
    public void runWeeklyTraining() {
        log.info("Iniciando Scheduled Task: Autoentrenamiento de Fraude");
        try {
            Map<String, Object> trainingRequest = buildTrainingRequest("scheduler-java");
            HttpEntity<Map<String, Object>> requestEntity = jsonEntity(trainingRequest);

            log.info("Llamando a Training API: {}", trainingApiUrl);
            ResponseEntity<Map> response = schedulerRestTemplate.postForEntity(trainingApiUrl, requestEntity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                persistAudit(response.getBody(), "Scheduled training result", "scheduler-java");
            } else {
                log.error("Error en Training API. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error ejecutando Scheduled Task de Fraude: {}", e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TAREA 2: Sensor de Drift — Diario a las 06:00 AM
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sensor Proactivo (PSI) + Sensor Reactivo (Recall).
     *
     * 1. Pide a Python el PSI de cada feature para las últimas 24h.
     * 2. Si algún feature crítico (amt, city_pop, age) tiene PSI > 0.25 → Dispara
     * entrenamiento.
     * 3. Si el Recall del último entrenamiento fue < 90% → Dispara entrenamiento.
     */
    @Scheduled(cron = "0 0 6 * * *")
    public void checkDriftSensors() {
        log.info("Iniciando Sensor de Drift Diario...");
        try {
            boolean mustRetrain = false;
            String retrainReason = null;

            // ── SENSOR PROACTIVO: PSI por Feature ────────────────────────────
            mustRetrain = checkPsiSensor();
            if (mustRetrain) {
                retrainReason = "DRIFT_PSI: PSI > " + PSI_THRESHOLD_CRITICAL + " en features críticos";
            }

            // ── SENSOR REACTIVO: Recall del último entrenamiento ─────────────
            if (!mustRetrain) {
                SelfTrainingAuditFraud lastAudit = auditRepository.findTopByIsSuccessTrueOrderByEndTrainingDesc();
                if (lastAudit != null && lastAudit.getRecallScore() != null) {
                    double lastRecall = lastAudit.getRecallScore().doubleValue();
                    log.info("[SENSOR RECALL] Último Recall registrado: {}", lastRecall);
                    if (lastRecall < RECALL_THRESHOLD_REACTIVE) {
                        mustRetrain = true;
                        retrainReason = "DRIFT_RECALL: Recall " + lastRecall
                                + " < umbral " + RECALL_THRESHOLD_REACTIVE;
                        log.warn("[SENSOR RECALL] Recall caído. Disparando reentrenamiento.");
                    } else {
                        log.info("[SENSOR RECALL] Recall aceptable: {} >= {}",
                                lastRecall, RECALL_THRESHOLD_REACTIVE);
                    }
                } else {
                    log.info("[SENSOR RECALL] Sin auditoría previa. Omitiendo evaluación.");
                }
            }

            // ── DISPARAR si algún sensor lo requiere ─────────────────────────
            if (mustRetrain) {
                log.warn("Reentrenamiento disparado. Razón: {}", retrainReason);
                triggerRetrainingByDrift(retrainReason);
            }

        } catch (Exception e) {
            log.error("Error en Sensor de Drift: {}", e.getMessage(), e);
        }
    }

    // ─── Implementación de los sensores ──────────────────────────────────────

    /**
     * Llama al Drift API, evalúa PSI y retorna true si debe reentrenarse.
     */
    private boolean checkPsiSensor() {
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // [DEMO] Ventana fija de datos históricos.
            // En producción: LocalDateTime.now().minusDays(1) / LocalDateTime.now()
            LocalDateTime driftWindowEnd = LocalDateTime.of(2019, 3, 31, 23, 59);
            LocalDateTime driftWindowStart = driftWindowEnd.minusDays(1);

            Map<String, Object> driftRequest = new HashMap<>();
            driftRequest.put("start_date", driftWindowStart.format(fmt));
            driftRequest.put("end_date", driftWindowEnd.format(fmt));
            driftRequest.put("persist", true);

            log.info("[SENSOR PSI] Llamando a Drift API: {} | Ventana: {} → {}",
                    driftApiUrl, driftWindowStart.format(fmt), driftWindowEnd.format(fmt));

            ResponseEntity<Map> driftResponse = schedulerRestTemplate.postForEntity(driftApiUrl,
                    jsonEntity(driftRequest), Map.class);

            if (!driftResponse.getStatusCode().is2xxSuccessful() || driftResponse.getBody() == null) {
                log.error("[SENSOR PSI] Drift API respondió con error: {}", driftResponse.getStatusCode());
                return false;
            }

            Map<String, Object> body = driftResponse.getBody();
            boolean hasCriticalDrift = Boolean.TRUE.equals(body.get("has_critical_drift"));

            if (!hasCriticalDrift) {
                log.info("[SENSOR PSI] Sin drift crítico. Sistema estable.");
                return false;
            }

            @SuppressWarnings("unchecked")
            List<String> criticalFeatures = (List<String>) body.getOrDefault("critical_features", List.of());
            log.info("[SENSOR PSI] PSI crítico en features: {}", criticalFeatures);

            boolean affectsCritical = criticalFeatures.stream()
                    .anyMatch(CRITICAL_FEATURES::contains);
            if (affectsCritical) {
                log.warn("[SENSOR PSI] Drift severo en features críticos: {}", criticalFeatures);
                return true;
            } else {
                log.info("[SENSOR PSI] Drift en features no críticos: {}. Sin disparo.", criticalFeatures);
                return false;
            }

        } catch (Exception e) {
            log.error("[SENSOR PSI] Error consultando Drift API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Dispara un ciclo de entrenamiento urgente cuando los sensores detectan drift.
     */
    private void triggerRetrainingByDrift(String driftReason) {
        log.info("[DRIFT TRIGGER] Iniciando reentrenamiento urgente. Razón: {}", driftReason);
        try {
            Map<String, Object> trainingRequest = buildTrainingRequest("drift-sensor");
            ResponseEntity<Map> response = schedulerRestTemplate
                    .postForEntity(trainingApiUrl, jsonEntity(trainingRequest), Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                persistAudit(response.getBody(), driftReason, "drift-sensor");
            }
        } catch (Exception e) {
            log.error("[DRIFT TRIGGER] Error disparando reentrenamiento: {}", e.getMessage(), e);
        }
    }

    private void triggerHotReload() {
        try {
            ResponseEntity<Map> reloadResponse = schedulerRestTemplate.postForEntity(predictionApiReloadUrl, null,
                    Map.class);
            if (reloadResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Hot-Reload exitoso en Prediction API.");
            } else {
                log.warn("Prediction API respondió con status: {}", reloadResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Falló llamada de Hot-Reload: {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Construye el Map de request de entrenamiento con las fechas del [DEMO].
     * En producción reemplazar con LocalDateTime.now().minusDays(30) / now().
     */
    private Map<String, Object> buildTrainingRequest(String triggeredBy) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // [DEMO] Fechas fijas del dataset histórico
        LocalDateTime start = LocalDateTime.of(2019, 7, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2019, 9, 30, 23, 59);

        Map<String, Object> request = new HashMap<>();
        request.put("start_date", start.format(fmt));
        request.put("end_date", end.format(fmt));
        request.put("optuna_trials", 20);
        request.put("undersampling_ratio", 4.0);
        request.put("triggered_by", triggeredBy);
        return request;
    }

    /**
     * Persiste el resultado de un ciclo de entrenamiento en la tabla de auditoría.
     */
    @SuppressWarnings("unchecked")
    private void persistAudit(Map<String, Object> body, String promotionReason, String triggeredBy) {
        String promotionStatus = (String) body.get("promotion_status");
        Map<String, Object> metrics = (Map<String, Object>) body.get("metrics");
        Map<String, Object> optunaResult = (Map<String, Object>) body.get("optuna_result");

        log.info("Entrenamiento completado. Status: {}", promotionStatus);

        SelfTrainingAuditFraud audit = new SelfTrainingAuditFraud();
        audit.setStartTraining(LocalDateTime.of(2019, 7, 1, 0, 0)); // [DEMO]
        audit.setEndTraining(LocalDateTime.now());
        audit.setPromotionStatus(promotionStatus);
        audit.setPromotionReason(promotionReason);
        audit.setIsSuccess(true);
        audit.setTriggeredBy(triggeredBy);

        if (metrics != null) {
            audit.setAccuracy(toBigDecimal(metrics.get("accuracy")));
            audit.setPrecisionScore(toBigDecimal(metrics.get("precision")));
            audit.setRecallScore(toBigDecimal(metrics.get("recall")));
            audit.setF1Score(toBigDecimal(metrics.get("f1_score")));
            audit.setAucRoc(toBigDecimal(metrics.get("auc_roc")));
            audit.setOptimalThreshold(toBigDecimal(metrics.get("optimal_threshold")));
        }
        if (optunaResult != null) {
            audit.setOptunaBestF1(toBigDecimal(optunaResult.get("best_f1_score")));
            if (optunaResult.get("best_params") != null) {
                audit.setOptunaBestParams(optunaResult.get("best_params").toString());
            }
        }

        auditRepository.save(audit);
        log.info("Auditoría guardada. ID: {}", audit.getIdAudit());

        if (FraudConstants.PROMO_PROMOTED.equalsIgnoreCase(promotionStatus)) {
            log.info("Modelo PROMOVIDO. Solicitando Hot-Reload a Prediction API...");
            triggerHotReload();
        }
    }

    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return null;
    }
}
