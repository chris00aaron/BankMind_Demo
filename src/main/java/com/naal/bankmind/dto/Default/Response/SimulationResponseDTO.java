package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para la respuesta de la simulación.
 * Idéntico a MorosidadResponseDTO.
 */
public record SimulationResponseDTO(
        @JsonProperty("default") Boolean isDefault,
        @JsonProperty("probabilidad_default") Double probabilidadDefault,
        @JsonProperty("main_risk_factor") String mainRiskFactor,
        @JsonProperty("model_version") String modelVersion) {
}
