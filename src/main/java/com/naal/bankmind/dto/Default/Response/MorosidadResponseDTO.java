package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * DTO para la respuesta de predicción de morosidad.
 */
public record MorosidadResponseDTO(
        @JsonProperty("default") Boolean isDefault,
        @JsonProperty("probabilidad_default") Double probabilidadDefault,
        @JsonProperty("main_risk_factor") String mainRiskFactor,
        @JsonProperty("risk_factors") List<RiskFactorDTO> riskFactors,
        @JsonProperty("model_version") String modelVersion) {
}
