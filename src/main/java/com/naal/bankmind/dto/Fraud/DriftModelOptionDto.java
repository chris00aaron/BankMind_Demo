package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Opción de modelo disponible para el selector del dashboard de PSI.
 *
 * La lista es curada por el backend:
 * - Siempre incluye el Champion activo (is_champion = true).
 * - Incluye únicamente los últimos N modelos PROMOTED (no REJECTED).
 * - Nunca supera el límite configurado (MAX_PROMOTED_OPTIONS).
 *
 * Esto asegura que el selector sea operativo y escalable incluso con
 * cientos de versiones de modelo registradas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriftModelOptionDto {

    @JsonProperty("id_model")
    private Long idModel;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("is_champion")
    private Boolean isChampion;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
