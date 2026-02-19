package com.naal.bankmind.dto.Default.Request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para una muestra de entrenamiento.
 * Coincide con el schema TrainingSample de la API Python.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrainingSampleDTO {

    @JsonProperty("LIMIT_BAL")
    private Double limitBal;

    @JsonProperty("SEX")
    private Integer sex;

    @JsonProperty("EDUCATION")
    private Integer education;

    @JsonProperty("MARRIAGE")
    private Integer marriage;

    @JsonProperty("AGE")
    private Integer age;

    @JsonProperty("PAY_0")
    private Integer pay0;

    @JsonProperty("PAY_2")
    private Integer pay2;

    @JsonProperty("PAY_3")
    private Integer pay3;

    @JsonProperty("PAY_4")
    private Integer pay4;

    @JsonProperty("PAY_5")
    private Integer pay5;

    @JsonProperty("PAY_6")
    private Integer pay6;

    @JsonProperty("BILL_AMT1")
    private Double billAmt1;

    @JsonProperty("BILL_AMT2")
    private Double billAmt2;

    @JsonProperty("BILL_AMT3")
    private Double billAmt3;

    @JsonProperty("BILL_AMT4")
    private Double billAmt4;

    @JsonProperty("BILL_AMT5")
    private Double billAmt5;

    @JsonProperty("BILL_AMT6")
    private Double billAmt6;

    @JsonProperty("PAY_AMT1")
    private Double payAmt1;

    @JsonProperty("PAY_AMT2")
    private Double payAmt2;

    @JsonProperty("PAY_AMT3")
    private Double payAmt3;

    @JsonProperty("PAY_AMT4")
    private Double payAmt4;

    @JsonProperty("PAY_AMT5")
    private Double payAmt5;

    @JsonProperty("PAY_AMT6")
    private Double payAmt6;

    @JsonProperty("UTILIZATION_RATE")
    private Double utilizationRate;

    @JsonProperty("default_payment_next_month")
    private Integer defaultPaymentNextMonth;

    @JsonProperty("sample_weight")
    private Double sampleWeight;
}
