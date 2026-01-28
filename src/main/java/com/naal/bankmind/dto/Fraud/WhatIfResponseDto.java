package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para respuesta de predicción What-If (simulación)
 * Incluye datos del cliente encontrado para mostrar en el frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfResponseDto {

    // Datos del cliente (para mostrar en UI)
    @JsonProperty("customer_found")
    private boolean customerFound;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("customer_location")
    private String customerLocation;

    @JsonProperty("customer_gender")
    private String customerGender;

    @JsonProperty("customer_age")
    private Integer customerAge;

    // Datos de la simulación
    @JsonProperty("simulated_amount")
    private Double simulatedAmount;

    @JsonProperty("simulated_category")
    private String simulatedCategory;

    @JsonProperty("simulated_hour")
    private Integer simulatedHour;

    // Resultado de predicción (de la API de IA)
    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("veredicto")
    private String veredicto;

    @JsonProperty("score_final")
    private Float scoreFinal;

    @JsonProperty("detalles_riesgo")
    private List<RiskFactorDto> detallesRiesgo;

    @JsonProperty("datos_auditoria")
    private Map<String, Object> datosAuditoria;

    @JsonProperty("recomendacion")
    private String recomendacion;

    // Mensaje de error si algo falla
    @JsonProperty("error")
    private String error;
}
