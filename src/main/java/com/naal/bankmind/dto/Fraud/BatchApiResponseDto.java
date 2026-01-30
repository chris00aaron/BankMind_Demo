package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO Response del lote de predicciones desde la API Python
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchApiResponseDto {

    @JsonProperty("total_processed")
    private int totalProcessed;

    @JsonProperty("total_frauds")
    private int totalFrauds;

    @JsonProperty("total_legitimate")
    private int totalLegitimate;

    @JsonProperty("total_errors")
    private int totalErrors;

    @JsonProperty("results")
    private List<FraudPredictionResponseDto> results;
}
