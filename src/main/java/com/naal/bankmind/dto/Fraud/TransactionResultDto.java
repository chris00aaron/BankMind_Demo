package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO unificado que contiene la transacción guardada + resultado del análisis
 * de fraude
 * Usado como respuesta del endpoint POST /api/transactions/process
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultDto {

    // ============ DATOS DE LA TRANSACCIÓN ============

    @JsonProperty("transaction_id")
    private Long transactionId;

    @JsonProperty("trans_num")
    private String transNum;

    @JsonProperty("amt")
    private Double amt;

    @JsonProperty("merchant")
    private String merchant;

    @JsonProperty("category")
    private String category;

    @JsonProperty("trans_date_time")
    private LocalDateTime transDateTime;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_id")
    private Long customerId;

    // ============ RESULTADO DEL ANÁLISIS DE FRAUDE ============

    @JsonProperty("fraud_analysis")
    private FraudPredictionResponseDto fraudAnalysis;

    @JsonProperty("processing_time_ms")
    private Long processingTimeMs;

    @JsonProperty("status")
    private String status; // "PROCESSED", "ERROR", "PENDING"
}
