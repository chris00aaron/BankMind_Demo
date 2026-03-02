package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de estadísticas temporales de fraude.
 *
 * Agrupa predicciones ALTO RIESGO por día de la semana y por mes,
 * para la gráfica "Cuándo ocurre el fraude" del Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemporalStatsDto {

    /**
     * Día de la semana ISO: 1 = Lunes … 7 = Domingo.
     * El frontend lo traduce a etiqueta legible.
     */
    @JsonProperty("day_of_week")
    private int dayOfWeek;

    /**
     * Etiqueta del mes en formato 'YYYY-MM'.
     * Ejemplo: '2026-02'
     */
    @JsonProperty("month_label")
    private String monthLabel;

    /** Cantidad de fraudes detectados en este día × mes */
    @JsonProperty("fraud_count")
    private long fraudCount;
}
