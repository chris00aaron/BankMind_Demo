package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO para recibir respuesta de la API de Fraude (Python/FastAPI)
 * Coincide con la estructura FraudOutput de la API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudPredictionResponseDto {

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

    @JsonProperty("error")
    private String error; // Para errores individuales en procesamiento batch
}
