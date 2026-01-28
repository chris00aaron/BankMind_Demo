package com.naal.bankmind.controller.Fraud;

import com.naal.bankmind.dto.Fraud.NewTransactionRequestDto;
import com.naal.bankmind.dto.Fraud.TransactionResultDto;
import com.naal.bankmind.service.Fraud.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para procesamiento de nuevas transacciones
 * Simula el flujo de un Punto de Venta (POS) o App móvil
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Endpoint principal para procesar una nueva transacción
     * 
     * Flujo:
     * 1. Recibe datos mínimos del POS (ccNum, amt, merchant, category, merchLat,
     * merchLong)
     * 2. Identifica al cliente por número de tarjeta
     * 3. Enriquece con datos del cliente (género, trabajo, ubicación, etc.)
     * 4. Guarda la transacción
     * 5. Llama a la API de IA para evaluar fraude
     * 6. Devuelve resultado unificado
     * 
     * POST /api/transactions/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> processTransaction(@Valid @RequestBody NewTransactionRequestDto request) {
        try {
            TransactionResultDto result = transactionService.processNewTransaction(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", e.getMessage(),
                            "status", "FAILED"));
        }
    }

    /**
     * Endpoint para simular múltiples transacciones de prueba
     * Útil para demos y testing
     * 
     * POST /api/transactions/simulate
     */
    @PostMapping("/simulate")
    public ResponseEntity<?> simulateTransaction(
            @RequestParam(defaultValue = "1234567890123456") Long ccNum,
            @RequestParam(defaultValue = "150.00") Double amount,
            @RequestParam(defaultValue = "grocery_pos") String category) {

        NewTransactionRequestDto request = NewTransactionRequestDto.builder()
                .ccNum(ccNum)
                .amt(amount)
                .merchant("Test Merchant")
                .category(category)
                .merchLat(-12.0463)
                .merchLong(-77.0427)
                .build();

        try {
            TransactionResultDto result = transactionService.processNewTransaction(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
