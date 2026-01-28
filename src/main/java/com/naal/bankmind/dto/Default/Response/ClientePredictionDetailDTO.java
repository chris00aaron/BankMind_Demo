package com.naal.bankmind.dto.Default.Response;

import java.math.BigDecimal;

/**
 * DTO completo con datos del cliente, cuenta y predicción para la vista de
 * detalle.
 */
public record ClientePredictionDetailDTO(
        // Datos del cliente
        Long idCustomer,
        String nombre,
        Integer edad,
        String educacion,
        String estadoCivil,
        String fechaRegistro,

        // Datos de la cuenta
        Long recordId,
        BigDecimal limitBal,
        BigDecimal balance,
        BigDecimal estimatedSalary,
        Integer tenure,

        // Datos calculados del historial
        Integer antiguedadMeses,
        Integer cuotasAtrasadas,
        Double historialPagos,
        BigDecimal ultimaCuota,
        String ultimoPago,

        // Predicción
        Boolean defaultPayment,
        Double probabilidadPago,
        String nivelRiesgo,
        String mainRiskFactor,
        String modelVersion,
        BigDecimal estimatedLoss) {
}
