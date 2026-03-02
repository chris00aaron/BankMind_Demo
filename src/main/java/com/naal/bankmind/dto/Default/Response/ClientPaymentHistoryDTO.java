package com.naal.bankmind.dto.Default.Response;

import java.math.BigDecimal;

/**
 * DTO para el historial de pagos mensual de un cliente.
 * Cada registro representa un mes del historial de la cuenta.
 */
public record ClientPaymentHistoryDTO(
        String period, // "Ene 2024" – formateado para la UI
        Integer payX, // Código original del modelo: -2, -1, 0, 1..9
        Integer monthsLate, // max(0, payX) — meses de retraso reales
        BigDecimal billAmt, // Monto facturado ese mes
        BigDecimal payAmt, // Monto pagado ese mes
        Boolean didPay, // ¿Pagó algo ese mes?
        Long daysLate, // Días entre expirationDate y actualPaymentDate (null si no hay fechas)
        String paymentStatus // "Sin consumo" | "A tiempo" | "Crédito renovable" | "N mes(es) de retraso"
) {
}
