package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UltimoEstadoAtmDetailsUseDTO(
    Long id,
    BigDecimal currentBalance,
    LocalDate lastDepositDate,
    LocalDate lastReloadDate,
    Long lastSyncId,
    LocalDate lastTransactionDate,
    LocalDate lastWithdrawalDate,
    LocalDateTime updatedAt,
    AtmDetailsDTO atmData
) {}
