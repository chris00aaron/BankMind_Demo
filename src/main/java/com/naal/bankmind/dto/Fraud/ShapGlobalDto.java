package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para factores SHAP globales (promedio de impacto por feature)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShapGlobalDto {

    @JsonProperty("feature_name")
    private String featureName;

    @JsonProperty("avg_impact")
    private Double avgImpact;

    @JsonProperty("occurrences")
    private Long occurrences;

    @JsonProperty("display_name")
    private String displayName;
}
