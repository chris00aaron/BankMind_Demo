package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para datos de tendencia horaria de transacciones/fraudes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyTrendDto {

    @JsonProperty("hour")
    private Integer hour;

    @JsonProperty("total_transactions")
    private Long totalTransactions;

    @JsonProperty("fraud_count")
    private Long fraudCount;

    @JsonProperty("fraud_rate")
    private Double fraudRate;
}
