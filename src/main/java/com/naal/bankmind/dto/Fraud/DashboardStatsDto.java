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
}
