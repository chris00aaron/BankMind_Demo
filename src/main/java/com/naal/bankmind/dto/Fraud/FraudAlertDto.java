package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para una alerta de fraude en la lista (vista resumida)
 * Contiene solo los datos esenciales para mostrar en la barra lateral
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlertDto {

    @JsonProperty("prediction_id")
    private Long predictionId;

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("transaction_db_id")
    private Long transactionDbId;

    @JsonProperty("veredicto")
    private String veredicto;

    @JsonProperty("score_final")
    private Float scoreFinal;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("merchant")
    private String merchant;

    @JsonProperty("category")
    private String category;

    @JsonProperty("customer_name")
    private String customerName;

    @JsonProperty("prediction_date")
    private LocalDateTime predictionDate;

    @JsonProperty("location")
    private String location;

    // Detalles SHAP solo cuando se solicita el detalle
    @JsonProperty("detalles_riesgo")
    private List<RiskFactorDto> detallesRiesgo;

    @JsonProperty("datos_auditoria")
    private AuditDataDto datosAuditoria;

    @JsonProperty("recomendacion")
    private String recomendacion;
}
