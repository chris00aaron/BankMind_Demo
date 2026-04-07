package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChurnBatchResultItemDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("churn_probability")
    private Double churnProbability;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("is_churn")
    private Integer isChurn;

    @JsonProperty("prediction_confidence")
    private Double predictionConfidence;

    @JsonProperty("model_version")
    private String modelVersion;
}
