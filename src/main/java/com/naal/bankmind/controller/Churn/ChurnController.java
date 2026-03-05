package com.naal.bankmind.controller.Churn;

import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
import com.naal.bankmind.dto.Churn.ChurnResponseDTO;
import com.naal.bankmind.dto.Churn.CampaignLogDTO;
import com.naal.bankmind.dto.Churn.SegmentDTO;
import com.naal.bankmind.dto.Churn.TrainResultDTO;
import com.naal.bankmind.dto.Churn.PerformanceStatusDTO;
import com.naal.bankmind.entity.ChurnPredictions;
import com.naal.bankmind.entity.RetentionStrategyDef;
import com.naal.bankmind.service.Churn.ChurnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Customer Churn Predictions.
 * 
 * Endpoints:
 * - GET /api/v1/churn/customers - Lists all customers for dashboard
 * - POST /api/v1/churn/analyze/{idCustomer} - Predicts and saves
 * - POST /api/v1/churn/simulate - What-If simulation without saving
 * - GET /api/v1/churn/history/{idCustomer} - Prediction history
 * - GET /api/v1/churn/recommendation/{idCustomer} - Next Best Action
 * - GET /api/v1/churn/geography - Geography statistics
 * - GET /api/v1/churn/mlops - MLOps metrics
 * - GET /api/v1/churn/campaigns - List campaigns (persisted)
 * - POST /api/v1/churn/campaigns - Create campaign (persisted)
 * - POST /api/v1/churn/train - Auto-training trigger
 * - GET /api/v1/churn/monitor/status - Performance monitor status
 * - POST /api/v1/churn/monitor/evaluate - Manual performance evaluation
 */
@RestController
@RequestMapping("/api/v1/churn")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174", "http://localhost:3000" })
public class ChurnController {

    private final ChurnService churnService;

    public ChurnController(ChurnService churnService) {
        this.churnService = churnService;
    }

    /**
     * GET /api/v1/churn/customers?page=0&size=50&search=&country=&riskLevel=
     * Lists customers paginated for dashboard display.
     * Returns a page of customers + global KPIs.
     *
     * @param country   Optional country filter (e.g. "Spain", "France")
     * @param riskLevel Optional risk band: "alto" (>70), "medio" (50-70), "bajo"
     *                  (<50)
     */
    @GetMapping("/customers")
    public com.naal.bankmind.dto.Churn.CustomerPageDTO getCustomersPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String riskLevel) {
        System.out.println("--> REQUEST RECEIVED: GET /api/v1/churn/customers?page=" + page + "&size=" + size
                + "&search=" + search + "&country=" + country + "&riskLevel=" + riskLevel);
        com.naal.bankmind.dto.Churn.CustomerPageDTO result = churnService.getCustomersPaginated(page, size, search,
                country, riskLevel);
        System.out.println("--> RESPONSE: Page " + page + " with " + result.getContent().size() + " customers (total: "
                + result.getTotalElements() + ")");
        return result;
    }

    /**
     * POST /api/v1/churn/analyze/{idCustomer}
     * Analyzes a real customer from DB and saves the prediction.
     * Returns the full DTO with XAI factors.
     */
    @PostMapping("/analyze/{idCustomer}")
    public ResponseEntity<ChurnResponseDTO> analyzeCustomer(@PathVariable Long idCustomer) {
        try {
            ChurnResponseDTO response = churnService.predictRealCustomer(idCustomer);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /api/v1/churn/simulate
     * Simulates a What-If scenario WITHOUT saving to DB.
     * Useful for analysts to test different configurations.
     */
    @PostMapping("/simulate")
    public ResponseEntity<ChurnResponseDTO> simulateScenario(@RequestBody ChurnRequestDTO simulatedData) {
        ChurnResponseDTO response = churnService.simulateScenario(simulatedData);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/churn/history/{idCustomer}
     * Gets the prediction history for a customer.
     * Useful for historical charts in the frontend.
     */
    @GetMapping("/history/{idCustomer}")
    public ResponseEntity<List<ChurnPredictions>> getHistory(@PathVariable Long idCustomer) {
        List<ChurnPredictions> history = churnService.getHistory(idCustomer);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/v1/churn/recommendation/{idCustomer}
     * Gets the Next Best Action strategy for a customer.
     */
    @GetMapping("/recommendation/{idCustomer}")
    public ResponseEntity<RetentionStrategyDef> getRecommendation(@PathVariable Long idCustomer) {
        try {
            RetentionStrategyDef strategy = churnService.getRecommendation(idCustomer);
            return ResponseEntity.ok(strategy);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/v1/churn/interact/{idCustomer}
     * Logs an interaction (e.g. email sent) for a customer.
     */
    @PostMapping("/interact/{idCustomer}")
    public ResponseEntity<Void> logInteraction(@PathVariable Long idCustomer, @RequestParam String actionType) {
        try {
            churnService.logInteraction(idCustomer, actionType);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/v1/churn/geography
     * Returns statistics grouped by country.
     */
    @GetMapping("/geography")
    public ResponseEntity<List<com.naal.bankmind.dto.Churn.GeographyStatsDTO>> getGeographyStats() {
        List<com.naal.bankmind.dto.Churn.GeographyStatsDTO> stats = churnService.getGeographyStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/v1/churn/mlops
     * Returns metrics for the ML model.
     */
    @GetMapping("/mlops")
    public ResponseEntity<com.naal.bankmind.dto.Churn.MLOpsMetricsDTO> getMLOpsMetrics() {
        com.naal.bankmind.dto.Churn.MLOpsMetricsDTO metrics = churnService.getMLOpsMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/v1/churn/config/segments
     * Returns all segment definitions for the rule engine.
     */
    @GetMapping("/config/segments")
    public ResponseEntity<List<SegmentDTO>> getSegments() {
        List<SegmentDTO> segments = churnService.getAllSegments();
        return ResponseEntity.ok(segments);
    }

    /**
     * POST /api/v1/churn/config/segments
     * Creates a new custom segment definition.
     */
    @PostMapping("/config/segments")
    public ResponseEntity<SegmentDTO> createSegment(@RequestBody SegmentDTO request) {
        try {
            SegmentDTO created = churnService.createSegment(request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            System.err.println("Error creating segment: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DELETE /api/v1/churn/config/segments/{id}
     * Deletes a segment definition by ID.
     */
    @DeleteMapping("/config/segments/{id}")
    public ResponseEntity<Void> deleteSegment(@PathVariable Integer id) {
        try {
            churnService.deleteSegment(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/churn/config/strategies
     * Returns all active retention strategies.
     */
    @GetMapping("/config/strategies")
    public ResponseEntity<List<RetentionStrategyDef>> getStrategies() {
        List<RetentionStrategyDef> strategies = churnService.getAllStrategies();
        return ResponseEntity.ok(strategies);
    }

    // ============================================================
    // CAMPAIGN MANAGEMENT (M2 — Real Persistence)
    // ============================================================

    /**
     * GET /api/v1/churn/campaigns
     * Returns all campaigns from the database.
     */
    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignLogDTO>> getCampaigns() {
        List<CampaignLogDTO> campaigns = churnService.getCampaigns();
        return ResponseEntity.ok(campaigns);
    }

    /**
     * POST /api/v1/churn/campaigns
     * Creates a new campaign and persists it in the database.
     */
    @PostMapping("/campaigns")
    public ResponseEntity<CampaignLogDTO> createCampaign(@RequestBody CampaignLogDTO request) {
        try {
            CampaignLogDTO result = churnService.createCampaign(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error creating campaign: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/v1/churn/train
     * Triggers the auto-training of the churn model.
     * Returns structured result with metrics from the training run.
     */
    @PostMapping("/train")
    public ResponseEntity<TrainResultDTO> trainModel() {
        System.out.println("--> REQUEST RECEIVED: POST /api/v1/churn/train");
        TrainResultDTO result = churnService.trainModel();
        System.out.println("--> TRAINING RESULT: " + result.getStatus());

        if ("error".equals(result.getStatus())) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    // ============================================================
    // PERFORMANCE MONITOR ENDPOINTS
    // ============================================================

    /**
     * GET /api/v1/churn/monitor/status
     * Returns the current performance monitor status.
     * Includes metrics from the last evaluation, next scheduled evaluation,
     * and configuration parameters.
     */
    @GetMapping("/monitor/status")
    public ResponseEntity<PerformanceStatusDTO> getMonitorStatus() {
        System.out.println("--> REQUEST RECEIVED: GET /api/v1/churn/monitor/status");
        PerformanceStatusDTO result = churnService.getPerformanceStatus();

        if ("error".equals(result.getStatus())) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/v1/churn/monitor/evaluate
     * Manually triggers a performance evaluation.
     * Compares historical predictions against ground truth and
     * triggers retraining if performance has degraded.
     */
    @PostMapping("/monitor/evaluate")
    public ResponseEntity<PerformanceStatusDTO> triggerEvaluation() {
        System.out.println("--> REQUEST RECEIVED: POST /api/v1/churn/monitor/evaluate");
        PerformanceStatusDTO result = churnService.triggerPerformanceEvaluation();
        System.out.println("--> EVALUATION RESULT: " + result.getStatus());

        if ("error".equals(result.getStatus())) {
            return ResponseEntity.internalServerError().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
