package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de fraude por categoría de comercio
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStatsDto {

    @JsonProperty("category")
    private String category;

    @JsonProperty("total_transactions")
    private Long totalTransactions;

    @JsonProperty("fraud_count")
    private Long fraudCount;

    @JsonProperty("fraud_rate")
    private Double fraudRate;

    @JsonProperty("total_amount")
    private Double totalAmount;
}
