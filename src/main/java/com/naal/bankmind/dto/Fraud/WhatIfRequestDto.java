package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para solicitar predicción What-If (simulación)
 * El frontend envía estos datos mínimos y el backend enriquece con datos del
 * cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfRequestDto {

    @JsonProperty("cc_num")
    private Long ccNum; // Número de tarjeta para identificar cliente

    @JsonProperty("amt")
    private Double amt; // Monto simulado

    @JsonProperty("merchant")
    private String merchant; // Nombre del comercio (requerido si se guarda en BD)

    @JsonProperty("category")
    private String category; // Categoría del comercio

    @JsonProperty("hour")
    private Integer hour; // Hora simulada (0-23)

    @JsonProperty("merch_lat")
    private Double merchLat; // Latitud comercio simulada

    @JsonProperty("merch_long")
    private Double merchLong; // Longitud comercio simulada

    @JsonProperty("save_to_db")
    @Builder.Default
    private Boolean saveToDB = false; // Si true, guarda la transacción y predicción en BD
}
