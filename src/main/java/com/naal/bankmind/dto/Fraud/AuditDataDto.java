package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para datos de auditoría del modelo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDataDto {

    @JsonProperty("xgboost_score")
    private Float xgboostScore;

    @JsonProperty("iforest_score")
    private Float iforestScore;

    @JsonProperty("base_score")
    private Float baseScore;

    @JsonProperty("prediction_id")
    private Long predictionId;
}
