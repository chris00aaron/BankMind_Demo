package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un cliente dentro de la muestra activa de Inteligencia de Riesgo.
 * 'analyzed' indica si tiene una predicción real del modelo (risk != 50 por defecto).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskClientDTO {
    private Long id;
    private String name;
    private Integer age;
    private Double balance;
    private String country;
    private Integer risk;      // Probabilidad de fuga 0-100. 50 si no analizado.
    private Boolean analyzed;  // true = tiene predicción real del modelo ML
    private Integer products;
    private Integer score;
    private String email;
}
