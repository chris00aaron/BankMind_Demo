package com.naal.bankmind.controller.Default;

import com.naal.bankmind.client.Default.MorosidadFeignClient;
import com.naal.bankmind.entity.Default.ModelMonitoringLog;
import com.naal.bankmind.entity.Default.ProductionModelDefault;
import com.naal.bankmind.entity.Default.TrainingHistory;
import com.naal.bankmind.repository.Default.ModelMonitoringLogRepository;
import com.naal.bankmind.repository.Default.ProductionModelDefaultRepository;
import com.naal.bankmind.repository.Default.TrainingHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Controller para exponer datos de monitoreo del modelo al frontend.
 * Unifica información de ProductionModelDefault, ModelMonitoringLog y
 * TrainingHistory.
 */
@Slf4j
@RestController
@RequestMapping("/api/model")
@RequiredArgsConstructor
public class ModelMonitoringController {

    private final ProductionModelDefaultRepository productionModelRepository;
    private final ModelMonitoringLogRepository monitoringLogRepository;
    private final TrainingHistoryRepository trainingHistoryRepository;
    private final MorosidadFeignClient morosidadFeignClient;

    // ═══════════════════════════════════════════
    // 1. MODELO EN PRODUCCIÓN
    // ═══════════════════════════════════════════

    /**
     * Retorna la información del modelo activo en producción.
     * GET /api/model/production
     */
    @GetMapping("/production")
    public ResponseEntity<Map<String, Object>> getProductionModel() {
        Optional<ProductionModelDefault> modelOpt = productionModelRepository.findByIsActiveTrue();

        if (modelOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "active", false,
                    "message", "No hay modelo en producción"));
        }

        ProductionModelDefault model = modelOpt.get();
        long daysActive = model.getDeploymentDate() != null
                ? ChronoUnit.DAYS.between(model.getDeploymentDate().toLocalDate(), LocalDate.now())
                : 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", true);
        response.put("version", model.getVersion());
        response.put("deploymentDate", model.getDeploymentDate());
        response.put("daysActive", daysActive);
        response.put("aucRoc", model.getAucRoc());
        response.put("giniCoefficient", model.getGiniCoefficient());
        response.put("ksStatistic", model.getKsStatistic());
        response.put("assemblyConfiguration", model.getAssemblyConfiguration());

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════
    // 2. LOGS DE DRIFT (PSI DIARIO)
    // ═══════════════════════════════════════════

    /**
     * Retorna logs de monitoreo de drift (PSI) de los últimos N días.
     * GET /api/model/monitoring/drift?days=30
     */
    @GetMapping("/monitoring/drift")
    public ResponseEntity<List<Map<String, Object>>> getDriftLogs(
            @RequestParam(value = "days", defaultValue = "30") int days) {

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<ModelMonitoringLog> logs = monitoringLogRepository
                .findByMonitoringDateBetweenOrderByMonitoringDateAsc(start, end);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelMonitoringLog logEntry : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monitoringDate", logEntry.getMonitoringDate());
            item.put("psiFeatures", logEntry.getPsiFeatures());
            item.put("driftDetected", logEntry.getDriftDetected());
            item.put("consecutiveDaysDrift", logEntry.getConsecutiveDaysDrift());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════
    // 3. VALIDACIONES MENSUALES (PREDICCIÓN VS REALIDAD)
    // ═══════════════════════════════════════════

    /**
     * Retorna logs de validación mensual (comparación predicción vs realidad).
     * GET /api/model/monitoring/validation
     */
    @GetMapping("/monitoring/validation")
    public ResponseEntity<List<Map<String, Object>>> getValidationLogs() {
        List<ModelMonitoringLog> logs = monitoringLogRepository.findByValidationStatus("VALIDATED");

        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelMonitoringLog logEntry : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("monitoringDate", logEntry.getMonitoringDate());
            item.put("aucRocReal", logEntry.getAucRocReal());
            item.put("ksReal", logEntry.getKsReal());
            item.put("predictedDefaultRate", logEntry.getPredictedDefaultRate());
            item.put("actualDefaultRate", logEntry.getActualDefaultRate());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════
    // 4. HISTORIAL DE ENTRENAMIENTOS
    // ═══════════════════════════════════════════

    /**
     * Retorna el historial completo de entrenamientos con métricas.
     * GET /api/model/training-history
     */
    @GetMapping("/training-history")
    public ResponseEntity<List<Map<String, Object>>> getTrainingHistory() {
        List<TrainingHistory> histories = trainingHistoryRepository.findAllByOrderByTrainingDateDesc();

        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingHistory th : histories) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("idTrainingHistory", th.getIdTrainingHistory());
            item.put("trainingDate", th.getTrainingDate());
            item.put("bestCadidateModel", th.getBestCadidateModel());
            item.put("inProduction", th.getInProduction());
            item.put("metricsResults", th.getMetricsResults());
            item.put("parametersOptuna", th.getParametersOptuna());

            // Info del dataset asociado
            if (th.getDatasetInfo() != null) {
                Map<String, Object> ds = new LinkedHashMap<>();
                ds.put("dataAmount", th.getDatasetInfo().getDataAmount());
                ds.put("dataTraining", th.getDatasetInfo().getDataTraining());
                ds.put("dataTesting", th.getDatasetInfo().getDataTesting());
                ds.put("creationDate", th.getDatasetInfo().getCreationDate());
                item.put("datasetInfo", ds);
            }

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // ═══════════════════════════════════════════
    // 5. VERIFICACIÓN DE VERSIÓN
    // ═══════════════════════════════════════════

    /**
     * Compara la versión del modelo en BD con la versión cargada en la API de
     * predicción.
     * GET /api/model/version-check
     */
    @GetMapping("/version-check")
    public ResponseEntity<Map<String, Object>> checkModelVersion() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Versión en BD
        Optional<ProductionModelDefault> modelOpt = productionModelRepository.findByIsActiveTrue();
        String bdVersion = modelOpt.map(ProductionModelDefault::getVersion).orElse("NO_MODEL");
        result.put("bdVersion", bdVersion);

        // Versión en API de predicción
        try {
            Map<String, Object> apiResponse = morosidadFeignClient.getModelVersion();
            String apiVersion = String.valueOf(apiResponse.getOrDefault("version", "UNKNOWN"));
            result.put("apiVersion", apiVersion);
            result.put("match", bdVersion.equals(apiVersion));
        } catch (Exception e) {
            log.warn("⚠️ No se pudo obtener versión de la API de predicción: {}", e.getMessage());
            result.put("apiVersion", "ERROR");
            result.put("match", false);
            result.put("error", "API de predicción no disponible");
        }

        return ResponseEntity.ok(result);
    }
}
