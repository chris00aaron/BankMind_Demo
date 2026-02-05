package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChurnRequestDTO {

    // Python espera "CreditScore" (Mayúscula inicial), Java usa "creditScore"
    @JsonProperty("CreditScore")
    private Integer creditScore;

    @JsonProperty("Geography")
    private String geography;

    @JsonProperty("Gender")
    private String gender;

    @JsonProperty("Age")
    private Integer age;

    @JsonProperty("Tenure")
    private Integer tenure;

    @JsonProperty("Balance")
    private Double balance;

    @JsonProperty("NumOfProducts")
    private Integer numOfProducts;

    @JsonProperty("HasCrCard")
    private Integer hasCrCard;

    @JsonProperty("IsActiveMember")
    private Integer isActiveMember;

    @JsonProperty("EstimatedSalary")
    private Double estimatedSalary;

    // Campo Ground Truth para validación (no para predicción)
    @JsonProperty("Exited")
    private Integer exited;
}