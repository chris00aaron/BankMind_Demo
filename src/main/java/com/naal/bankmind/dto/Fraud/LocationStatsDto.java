package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para estadísticas de fraude por ubicación geográfica
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationStatsDto {

    @JsonProperty("state")
    private String state;

    @JsonProperty("city")
    private String city;

    @JsonProperty("fraud_count")
    private Long fraudCount;

    @JsonProperty("total_transactions")
    private Long totalTransactions;

    @JsonProperty("fraud_rate")
    private Double fraudRate;
}
