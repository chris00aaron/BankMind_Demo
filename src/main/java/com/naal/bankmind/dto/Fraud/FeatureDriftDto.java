package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de una medición de PSI por feature.
 * Alimenta la gráfica de "Evolución del PSI" en el Frontend.
 * Eje X = measured_at, Eje Y = psi_value, Líneas = feature_name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureDriftDto {

    @JsonProperty("id_drift")
    private Long idDrift;

    @JsonProperty("id_model")
    private Long idModel;

    @JsonProperty("feature_name")
    private String featureName;

    @JsonProperty("psi_value")
    private BigDecimal psiValue;

    @JsonProperty("drift_category")
    private String driftCategory; // LOW | MODERATE | HIGH

    @JsonProperty("measured_at")
    private LocalDateTime measuredAt;
}
