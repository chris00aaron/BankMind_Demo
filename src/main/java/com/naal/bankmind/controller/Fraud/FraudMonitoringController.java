package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.dto.Fraud.ChampionModelDto;
import com.naal.bankmind.dto.Fraud.DriftModelOptionDto;
import com.naal.bankmind.dto.Fraud.FeatureDriftDto;
import com.naal.bankmind.dto.Fraud.TrainingAuditDto;
import com.naal.bankmind.service.Fraud.FraudMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller para el panel de Monitoreo del Modelo de Fraude.
 *
 * Endpoints:
 * GET /api/fraud/monitoring/champion → Modelo CHAMPION activo
 * GET /api/fraud/monitoring/history?limit=10 → Historial de entrenamientos
 * GET /api/fraud/monitoring/drift?days=30 → Historial de PSI por feature
 * GET /api/fraud/monitoring/drift/latest → Último PSI por feature (estado
 * actual)
 * POST /api/fraud/monitoring/train/manual → Disparar entrenamiento manual
 */
@RestController
@RequestMapping("/api/fraud/monitoring")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
@RequiredArgsConstructor
public class FraudMonitoringController {

    private final FraudMonitoringService monitoringService;

    /**
     * GET /api/fraud/monitoring/champion
     * Devuelve el modelo activo con sus métricas del último entrenamiento.
     */
    @GetMapping("/champion")
    public ResponseEntity<?> getChampionModel() {
        try {
            ChampionModelDto champion = monitoringService.getChampionModel();
            if (champion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No hay modelo CHAMPION activo"));
            }
            return ResponseEntity.ok(champion);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/fraud/monitoring/history?limit=10
     * Devuelve el historial de ciclos de entrenamiento, con métricas.
     */
    @GetMapping("/history")
    public ResponseEntity<List<TrainingAuditDto>> getTrainingHistory(
            @RequestParam(defaultValue = "10") int limit) {
        List<TrainingAuditDto> history = monitoringService.getTrainingHistory(limit);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/fraud/monitoring/drift?days=30[&modelId=5]
     *
     * Devuelve el historial de PSI para la gráfica de líneas.
     * - Sin modelId (default): cross-model, línea continua a través de versiones.
     * - Con modelId: filtra solo ese modelo (modo auditoría).
     */
    @GetMapping("/drift")
    public ResponseEntity<List<FeatureDriftDto>> getDriftHistory(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long modelId) {
        List<FeatureDriftDto> drift = monitoringService.getDriftHistory(days, modelId);
        return ResponseEntity.ok(drift);
    }

    /**
     * GET /api/fraud/monitoring/drift/latest[?modelId=5]
     * Devuelve el último PSI medido por feature.
     * - Sin modelId: cross-model (PSI más reciente global por feature).
     * - Con modelId: solo el último medido para ese modelo.
     */
    @GetMapping("/drift/latest")
    public ResponseEntity<List<FeatureDriftDto>> getLatestDrift(
            @RequestParam(required = false) Long modelId) {
        List<FeatureDriftDto> latest = monitoringService.getLatestDriftPerFeature(modelId);
        return ResponseEntity.ok(latest);
    }

    /**
     * GET /api/fraud/monitoring/drift/models
     *
     * Lista curada de modelos para el selector del gráfico PSI.
     * Devuelve: champion activo + últimos 5 PROMOTED, nunca REJECTED.
     * El backend filtra para que el frontend no reciba nunca más de 6 opciones,
     * independientemente de cuántos modelos existan en la BD.
     */
    @GetMapping("/drift/models")
    public ResponseEntity<List<DriftModelOptionDto>> getDriftModelOptions() {
        return ResponseEntity.ok(monitoringService.getDriftModelOptions());
    }

    /**
     * POST /api/fraud/monitoring/train/manual
     * Body: { "start_date": "2019-01-01", "end_date": "2019-03-31" }
     * Dispara un entrenamiento manual enviando la solicitud directamente a la
     * Training API.
     */
    @PostMapping("/train/manual")
    public ResponseEntity<?> triggerManualTraining(@RequestBody Map<String, String> request) {
        String startDate = request.get("start_date");
        String endDate = request.get("end_date");

        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Se requieren 'start_date' y 'end_date'"));
        }

        try {
            Map<String, Object> result = monitoringService.triggerManualTraining(startDate, endDate);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
