package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO consolidado con estadísticas del dashboard de fraude
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {

    @JsonProperty("transactions_today")
    private Long transactionsToday;

    @JsonProperty("frauds_detected")
    private Long fraudsDetected;

    @JsonProperty("legitimate")
    private Long legitimate;

    @JsonProperty("fraud_rate")
    private Double fraudRate;

    @JsonProperty("total_amount_at_risk")
    private Double totalAmountAtRisk;

    @JsonProperty("avg_fraud_score")
    private Double avgFraudScore;

    // ========== NUEVOS CAMPOS: Sistema de Notificaciones ==========

    // Estados de transacciones
    @JsonProperty("pending_count")
    private Long pendingCount; // Transacciones esperando respuesta del cliente

    @JsonProperty("approved_count")
    private Long approvedCount; // Transacciones aprobadas

    @JsonProperty("rejected_count")
    private Long rejectedCount; // Transacciones rechazadas por fraude

    // Tarjetas bloqueadas
    @JsonProperty("cards_blocked_today")
    private Long cardsBlockedToday; // Tarjetas bloqueadas hoy por fraude confirmado
}
