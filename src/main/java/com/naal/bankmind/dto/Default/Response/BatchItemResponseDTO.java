package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO para una predicción individual dentro de la respuesta batch.
 * Los campos deben mapear con el JSON de Python (snake_case).
 */
@Data
public class BatchItemResponseDTO {

    private int index;

    @JsonProperty("default")
    private boolean defaultPayment;

    @JsonProperty("probabilidad_default")
    private double probabilidadDefault;

    @JsonProperty("main_risk_factor")
    private String mainRiskFactor;
}
