package com.naal.bankmind.dto.Default.Response;

import java.math.BigDecimal;
import java.util.List;

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
                String mainRiskFactor,
                List<RiskFactorDTO> riskFactors, // Top 5 factores SHAP
                String modelVersion,
                BigDecimal estimatedLoss,

                // Clasificación SBS y comparación
                String clasificacionSBS, // Normal, CPP, Deficiente, Dudoso, Pérdida
                Integer percentilRiesgo, // Posición relativa vs cartera (0-100)
                Double umbralPolitica // Umbral de la política activa (ej: 50.0)
) {
}
