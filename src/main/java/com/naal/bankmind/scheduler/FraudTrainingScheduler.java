package com.naal.bankmind.scheduler;

import com.naal.bankmind.entity.SelfTrainingAuditFraud;
import com.naal.bankmind.repository.Fraud.SelfTrainingAuditFraudRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class FraudTrainingScheduler {

    @Autowired
    private SelfTrainingAuditFraudRepository auditRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // URLs de las APIs
    private static final String TRAINING_API_URL = "http://localhost:8001/fraude/train";
    private static final String PREDICTION_API_RELOAD_URL = "http://localhost:8000/api/v1/fraud/reload";

    // Cron: Lunes a las 2:00 AM
    @Scheduled(cron = "0 0 2 * * MON")
    public void runWeeklyTraining() {
        log.info("🕒 Iniciando Scheduled Task: Autoentrenamiento de Fraude");

        try {
            // 1. Preparar Request para Training API
            Map<String, Object> trainingRequest = new HashMap<>();

            // Fechas: Últimos 30 días
            // LocalDateTime now = LocalDateTime.now();
            // LocalDateTime start = now.minusDays(30);
            // [DEMO] Usar datos históricos de 2019
            LocalDateTime start = LocalDateTime.of(2019, 1, 1, 0, 0);
            LocalDateTime now = LocalDateTime.of(2019, 3, 31, 23, 59);
            // LocalDateTime now = LocalDateTime.now();
            // LocalDateTime start = now.minusDays(30);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            trainingRequest.put("start_date", start.format(fmt));
            trainingRequest.put("end_date", now.format(fmt));
            trainingRequest.put("optuna_trials", 20); // Valor por defecto
            trainingRequest.put("undersampling_ratio", 4.0); // Valor por defecto
            trainingRequest.put("triggered_by", "scheduler-java");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(trainingRequest, headers);

            // 2. Llamar a Training API
            log.info("🚀 Llamando a Training API: {}", TRAINING_API_URL);
            ResponseEntity<Map> response = restTemplate.postForEntity(TRAINING_API_URL, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();

                // 3. Extraer datos relevantes
                String promotionStatus = (String) body.get("promotion_status");
                Map metrics = (Map) body.get("metrics");
                Map optunaResult = (Map) body.get("optuna_result");

                log.info("✅ Entrenamiento completado. Status: {}", promotionStatus);

                // 4. Guardar en Auditoría Java
                SelfTrainingAuditFraud audit = new SelfTrainingAuditFraud();
                audit.setStartTraining(start); // Usamos la fecha de inicio del rango
                audit.setEndTraining(LocalDateTime.now());
                audit.setPromotionStatus(promotionStatus);
                audit.setPromotionReason("Scheduled training result");
                audit.setIsSuccess(true);
                audit.setTriggeredBy("scheduler-java");

                if (metrics != null) {
                    // NOTA: Cambiamos a los nuevos nombres de setters sincronizados con SQL
                    audit.setAccuracy(toBigDecimal(metrics.get("accuracy")));
                    audit.setPrecisionScore(toBigDecimal(metrics.get("precision"))); // Coincide con precision_score
                    audit.setRecallScore(toBigDecimal(metrics.get("recall"))); // Coincide con recall_score
                    audit.setF1Score(toBigDecimal(metrics.get("f1_score")));
                    audit.setAucRoc(toBigDecimal(metrics.get("auc_roc")));
                    audit.setOptimalThreshold(toBigDecimal(metrics.get("optimal_threshold")));
                }

                if (optunaResult != null) {
                    // Sincronizado con la columna optuna_best_params
                    audit.setOptunaBestF1(toBigDecimal(optunaResult.get("best_f1_score")));
                    audit.setOptunaBestParams(optunaResult.get("best_params").toString());
                }

                auditRepository.save(audit);
                log.info("💾 Auditoría guardada exitosamente sin redundancias. ID: {}", audit.getIdAudit());

                // 5. Si hubo promoción, recargar Prediction API
                if ("PROMOTED".equalsIgnoreCase(promotionStatus)) {
                    log.info("🏆 Modelo PROMOVIDO. Solicitando Hot-Reload a Prediction API...");
                    triggerHotReload();
                } else {
                    log.info("⏹️ Modelo NO promovido. No se requiere recarga.");
                }

            } else {
                log.error("❌ Error en Training API. Status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error ejecutando Scheduled Task de Fraude: {}", e.getMessage(), e);
        }
    }

    private void triggerHotReload() {
        try {
            ResponseEntity<Map> reloadResponse = restTemplate.postForEntity(PREDICTION_API_RELOAD_URL, null, Map.class);
            if (reloadResponse.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Hot-Reload exitoso en Prediction API.");
            } else {
                log.warn("⚠️ Prediction API respondió con status: {}", reloadResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("❌ Falló llamanda de Hot-Reload: {}", e.getMessage());
        }
    }

    private java.math.BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return null;
    }

}
