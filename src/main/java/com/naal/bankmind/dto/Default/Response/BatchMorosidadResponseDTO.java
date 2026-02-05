package com.naal.bankmind.dto.Default.Response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * DTO para la respuesta batch de la API Python de morosidad.
 */
@Data
public class BatchMorosidadResponseDTO {

    private List<BatchItemResponseDTO> predictions;

    @JsonProperty("shap_summary")
    private List<RiskFactorDTO> shapSummary;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("total_processed")
    private int totalProcessed;
}
