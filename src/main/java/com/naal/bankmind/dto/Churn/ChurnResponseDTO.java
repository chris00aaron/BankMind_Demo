package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO to receive response from the Python Churn Prediction API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnResponseDTO {

    @JsonProperty("churn_probability")
    private Double churnProbability;

    @JsonProperty("is_churn")
    private Boolean isChurn;

    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("prediction_confidence")
    private Double predictionConfidence;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("risk_factors")
    private List<RiskFactorDTO> riskFactors;

    // Campo para devolver el Ground Truth al frontend
    @JsonProperty("real_exit")
    private Boolean realExit;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskFactorDTO {
        private String feature;
        private Double impact;
        private String type;
    }
}
