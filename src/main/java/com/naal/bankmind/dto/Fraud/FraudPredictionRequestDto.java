package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para enviar datos de transacción a la API de Fraude (Python/FastAPI)
 * Estructura requerida por el endpoint POST /api/v1/fraud/predict
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudPredictionRequestDto {

    @JsonProperty("transaction_id")
    private String transactionId;

    @JsonProperty("id_cliente")
    private String idCliente;

    @JsonProperty("trans_date_trans_time")
    private String transDateTransTime;

    @JsonProperty("amt")
    private Double amt;

    @JsonProperty("category")
    private String category;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("job")
    private String job;

    @JsonProperty("city_pop")
    private Integer cityPop;

    @JsonProperty("dob")
    private String dob;

    @JsonProperty("lat")
    private Double lat;

    @JsonProperty("long")
    private Double lng;

    @JsonProperty("merch_lat")
    private Double merchLat;

    @JsonProperty("merch_long")
    private Double merchLong;
}
