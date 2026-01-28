package com.naal.bankmind.dto.Default.Response;

import java.math.BigDecimal;

/**
 * DTO resumen de una cuenta para mostrar en la lista de cuentas del cliente.
 */
public record AccountSummaryDTO(
        Long recordId,
        BigDecimal limitBal,
        BigDecimal balance,
        Integer tenure,
        Boolean isActiveMember) {
}
