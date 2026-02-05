package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para la respuesta de la simulación.
 * Incluye factores SHAP, pérdida estimada y clasificación SBS.
 */
public record SimulationResponseDTO(
                @JsonProperty("default") Boolean isDefault,
                @JsonProperty("probabilidad_default") Double probabilidadDefault,
                @JsonProperty("main_risk_factor") String mainRiskFactor,
                @JsonProperty("risk_factors") List<RiskFactorDTO> riskFactors,
                @JsonProperty("estimated_loss") BigDecimal estimatedLoss,
                @JsonProperty("umbral_politica") Double umbralPolitica,
                @JsonProperty("clasificacion_sbs") String clasificacionSBS,
                @JsonProperty("model_version") String modelVersion) {
}
