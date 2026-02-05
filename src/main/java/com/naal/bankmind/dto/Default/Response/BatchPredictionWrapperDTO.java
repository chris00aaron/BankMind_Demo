package com.naal.bankmind.dto.Default.Response;

import java.util.List;
import com.naal.bankmind.dto.Default.Response.RiskFactorDTO;

/**
 * Wrapper para respuesta batch que incluye predicciones y metadatos.
 */
public record BatchPredictionWrapperDTO(
                List<BatchAccountPredictionDTO> predictions,
                Double umbralPolitica, // Umbral de la política activa (0-100)
                List<RiskFactorDTO> shapSummary // Resumen agregado de SHAP (opcional)
) {
}
