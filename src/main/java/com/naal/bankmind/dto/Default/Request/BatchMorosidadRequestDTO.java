package com.naal.bankmind.dto.Default.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO para enviar múltiples requests de morosidad a la API Python.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchMorosidadRequestDTO {

    private List<MorosidadRequestDTO> items;

    @com.fasterxml.jackson.annotation.JsonProperty("include_shap")
    private boolean includeShap;
}
