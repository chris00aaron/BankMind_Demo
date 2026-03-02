package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.dto.Fraud.*;
import com.naal.bankmind.dto.Fraud.DemographicStatsDto;
import com.naal.bankmind.dto.Fraud.TemporalStatsDto;
import com.naal.bankmind.service.Fraud.FraudAlertService;
import com.naal.bankmind.service.Fraud.FraudPredictionService;
import com.naal.bankmind.service.Fraud.FraudStatsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para operaciones de detección de fraude
 * Delega a servicios especializados siguiendo principios SOLID
 */
@RestController
@RequestMapping("/api/fraud")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:3000" })
public class FraudController {

    private final FraudPredictionService predictionService;
    private final FraudAlertService alertService;
    private final FraudStatsService statsService;

    public FraudController(
            FraudPredictionService predictionService,
            FraudAlertService alertService,
            FraudStatsService statsService) {
        this.predictionService = predictionService;
        this.alertService = alertService;
        this.statsService = statsService;
    }

    // ==================== ENDPOINTS DE ALERTAS ====================

    /**
     * GET /api/fraud/alerts - Obtener alertas paginadas con filtros
     */
    @GetMapping("/alerts")
    public ResponseEntity<FraudAlertsPageDto> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String veredicto,
            @RequestParam(required = false) String search) {

        FraudAlertsPageDto alerts = alertService.getAlertsPage(page, size, sortBy, order, veredicto, search);
        return ResponseEntity.ok(alerts);
    }

    /**
     * GET /api/fraud/alerts/{predictionId} - Obtener detalle de una alerta
     */
    @GetMapping("/alerts/{predictionId}")
    public ResponseEntity<?> getAlertDetail(@PathVariable Long predictionId) {
        try {
            FraudAlertDto alert = alertService.getAlertDetail(predictionId);
            return ResponseEntity.ok(alert);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== ENDPOINTS DE PREDICCIÓN ====================

    /**
     * POST /api/fraud/predict/{transactionId} - Realizar predicción de fraude
     */
    @PostMapping("/predict/{transactionId}")
    public ResponseEntity<?> predictFraud(@PathVariable Long transactionId) {
        try {
            FraudPredictionResponseDto response = predictionService.predictTransaction(transactionId);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/fraud/predictions/transaction/{transactionId} - Obtener predicción
     * existente
     */
    @GetMapping("/predictions/transaction/{transactionId}")
    public ResponseEntity<?> getPredictionByTransaction(@PathVariable Long transactionId) {
        try {
            FraudPredictionResponseDto response = predictionService.getPredictionByTransactionId(transactionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/fraud/predictions - Listar todas las predicciones
     */
    @GetMapping("/predictions")
    public ResponseEntity<List<FraudPredictionResponseDto>> getAllPredictions() {
        List<FraudPredictionResponseDto> predictions = predictionService.getAllPredictions();
        return ResponseEntity.ok(predictions);
    }

    /**
     * GET /api/fraud/predictions/filter - Filtrar predicciones por veredicto
     */
    @GetMapping("/predictions/filter")
    public ResponseEntity<List<FraudPredictionResponseDto>> getPredictionsByVeredicto(
            @RequestParam String veredicto) {
        List<FraudPredictionResponseDto> predictions = predictionService.getPredictionsByVeredicto(veredicto);
        return ResponseEntity.ok(predictions);
    }

    // ==================== ENDPOINTS DE ESTADÍSTICAS (DASHBOARD)
    // ====================

    /**
     * GET /api/fraud/stats/summary - Estadísticas consolidadas
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(statsService.getDashboardStats());
    }

    /**
     * GET /api/fraud/stats/hourly - Tendencia horaria
     */
    @GetMapping("/stats/hourly")
    public ResponseEntity<List<HourlyTrendDto>> getHourlyTrend() {
        return ResponseEntity.ok(statsService.getHourlyTrend());
    }

    /**
     * GET /api/fraud/stats/shap-global - Factores SHAP globales
     */
    @GetMapping("/stats/shap-global")
    public ResponseEntity<List<ShapGlobalDto>> getGlobalShapStats() {
        return ResponseEntity.ok(statsService.getGlobalShapStats());
    }

    /**
     * GET /api/fraud/stats/categories - Estadísticas por categoría
     */
    @GetMapping("/stats/categories")
    public ResponseEntity<List<CategoryStatsDto>> getCategoryStats() {
        return ResponseEntity.ok(statsService.getCategoryStats());
    }

    /**
     * GET /api/fraud/stats/locations - Estadísticas por ubicación
     */
    @GetMapping("/stats/locations")
    public ResponseEntity<List<LocationStatsDto>> getLocationStats() {
        return ResponseEntity.ok(statsService.getLocationStats());
    }

    // ==================== HEALTH CHECK ====================

    /**
     * GET /api/fraud/health - Verificar disponibilidad de la API de fraude
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkApiHealth() {
        boolean isAvailable = predictionService.isApiAvailable();
        return ResponseEntity.ok(Map.of(
                "fraud_api_available", isAvailable,
                "status", isAvailable ? "UP" : "DOWN"));
    }

    // ==================== ANALYTICS ====================

    /**
     * GET /api/fraud/stats/demographics - Perfil demográfico del defraudador
     * Devuelve fraudes agrupados por género × rango de edad.
     */
    @GetMapping("/stats/demographics")
    public ResponseEntity<List<DemographicStatsDto>> getDemographicStats() {
        return ResponseEntity.ok(statsService.getDemographicStats());
    }

    /**
     * GET /api/fraud/stats/temporal - Distribución temporal del fraude
     * Devuelve fraudes agrupados por día de la semana × mes.
     */
    @GetMapping("/stats/temporal")
    public ResponseEntity<List<TemporalStatsDto>> getTemporalStats() {
        return ResponseEntity.ok(statsService.getTemporalStats());
    }
}
