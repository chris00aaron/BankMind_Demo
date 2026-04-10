package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.dto.Fraud.BatchResultDto;
import com.naal.bankmind.dto.Fraud.PendingTransactionDto;
import com.naal.bankmind.service.Fraud.BatchPredictionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para procesamiento de predicciones por lotes
 */
@RestController
@RequestMapping("/api/fraud/batch")
public class BatchController {

    private final BatchPredictionService batchPredictionService;

    public BatchController(BatchPredictionService batchPredictionService) {
        this.batchPredictionService = batchPredictionService;
    }

    /**
     * GET /api/fraud/batch/pending/count - Obtener conteo de transacciones
     * pendientes
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingCount() {
        long count = batchPredictionService.getPendingCount();
        return ResponseEntity.ok(Map.of("pending_count", count));
    }

    /**
     * GET /api/fraud/batch/pending - Obtener lista de transacciones pendientes
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingTransactionDto>> getPendingTransactions(
            @RequestParam(defaultValue = "100") int limit) {
        List<PendingTransactionDto> pending = batchPredictionService.getPendingTransactions(limit);
        return ResponseEntity.ok(pending);
    }

    /**
     * POST /api/fraud/batch/process - Procesar lote por IDs específicos
     */
    @PostMapping("/process")
    public ResponseEntity<BatchResultDto> processBatch(@RequestBody List<Long> transactionIds) {
        BatchResultDto result = batchPredictionService.processBatch(transactionIds);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/fraud/batch/process-next - Procesar automáticamente las siguientes
     * N pendientes
     */
    @PostMapping("/process-next")
    public ResponseEntity<BatchResultDto> processNextBatch(
            @RequestParam(defaultValue = "100") int limit) {
        BatchResultDto result = batchPredictionService.processNextBatch(limit);
        return ResponseEntity.ok(result);
    }
}
