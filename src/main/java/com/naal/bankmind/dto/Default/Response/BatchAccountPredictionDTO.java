package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * DTO para la respuesta de predicción por lotes.
 * Contiene datos del cliente, cuenta y predicción.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchAccountPredictionDTO {

    // Datos del cliente
    private Long idCustomer;
    private String nombre;
    private Integer edad;
    private String educacion;
    private String estadoCivil;

    // Datos de la cuenta
    private Long recordId;
    private BigDecimal limitBal;
    private BigDecimal balance;

    // Predicción
    private Double probabilidadPago;
    private String nivelRiesgo;
    private BigDecimal montoCuota;
}
