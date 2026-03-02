package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de estadísticas demográficas de fraude.
 *
 * Agrupa predicciones ALTO RIESGO por género y rango de edad,
 * para la gráfica "Perfil del Defraudador" del Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemographicStatsDto {

    /** 'Masculino' | 'Femenino' */
    @JsonProperty("gender_label")
    private String genderLabel;

    /** '18-30' | '31-45' | '46-60' | '60+' */
    @JsonProperty("age_band")
    private String ageBand;

    /** Cantidad de fraudes en este segmento */
    @JsonProperty("fraud_count")
    private long fraudCount;
}
