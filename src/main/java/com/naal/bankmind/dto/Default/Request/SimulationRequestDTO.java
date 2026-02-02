package com.naal.bankmind.dto.Default.Request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para la solicitud de simulación de predicción.
 * Es idéntico a MorosidadRequestDTO pero se usa específicamente para
 * simulaciones
 * que no se persistirán en la base de datos.
 */
public record SimulationRequestDTO(
        @JsonProperty("LIMIT_BAL") Double limitBal,
        @JsonProperty("SEX") Integer sex,
        @JsonProperty("EDUCATION") Integer education,
        @JsonProperty("MARRIAGE") Integer marriage,
        @JsonProperty("AGE") Integer age,

        // Estado de pago últimos 6 meses
        @JsonProperty("PAY_0") Integer pay0,
        @JsonProperty("PAY_2") Integer pay2,
        @JsonProperty("PAY_3") Integer pay3,
        @JsonProperty("PAY_4") Integer pay4,
        @JsonProperty("PAY_5") Integer pay5,
        @JsonProperty("PAY_6") Integer pay6,

        // Monto de factura últimos 6 meses
        @JsonProperty("BILL_AMT1") Double billAmt1,
        @JsonProperty("BILL_AMT2") Double billAmt2,
        @JsonProperty("BILL_AMT3") Double billAmt3,
        @JsonProperty("BILL_AMT4") Double billAmt4,
        @JsonProperty("BILL_AMT5") Double billAmt5,
        @JsonProperty("BILL_AMT6") Double billAmt6,

        // Monto de pago últimos 6 meses
        @JsonProperty("PAY_AMT1") Double payAmt1,
        @JsonProperty("PAY_AMT2") Double payAmt2,
        @JsonProperty("PAY_AMT3") Double payAmt3,
        @JsonProperty("PAY_AMT4") Double payAmt4,
        @JsonProperty("PAY_AMT5") Double payAmt5,
        @JsonProperty("PAY_AMT6") Double payAmt6,

        @JsonProperty("UTILIZATION_RATE") Double utilizationRate) {
}
