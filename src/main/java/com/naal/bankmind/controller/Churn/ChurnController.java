package com.naal.bankmind.controller.Churn;

import com.naal.bankmind.dto.Churn.ChurnRequestDTO;
import com.naal.bankmind.dto.Churn.ChurnResponseDTO;
import com.naal.bankmind.dto.Churn.CustomerDashboardDTO;
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
 * - GET /api/v1/churn/financial-impact - Financial Dashboard Data
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
     * GET /api/v1/churn/customers
     * Lists all customers for dashboard display.
     */
    @GetMapping("/customers")
    public List<CustomerDashboardDTO> getAllCustomersForDashboard() {
        System.out.println("--> REQUEST RECEIVED: GET /api/v1/churn/customers");
        List<CustomerDashboardDTO> customers = churnService.getAllCustomersForDashboard();
        System.out.println("--> RESPONSE: Found " + customers.size() + " customers.");
        return customers;
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
    public ResponseEntity<List<com.naal.bankmind.dto.Churn.SegmentDTO>> getSegments() {
        List<com.naal.bankmind.dto.Churn.SegmentDTO> segments = churnService.getAllSegments();
        return ResponseEntity.ok(segments);
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
}
