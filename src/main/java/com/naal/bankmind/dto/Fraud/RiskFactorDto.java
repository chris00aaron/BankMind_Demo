package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para detalles de riesgo basados en valores SHAP
 * Coincide con la estructura RiskFactor de la API de Python
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactorDto {

    @JsonProperty("feature_name")
    private String featureName;

    @JsonProperty("feature_value")
    private String featureValue;

    @JsonProperty("shap_value")
    private Float shapValue;

    @JsonProperty("risk_description")
    private String riskDescription;

    @JsonProperty("impact_direction")
    private String impactDirection;
}
