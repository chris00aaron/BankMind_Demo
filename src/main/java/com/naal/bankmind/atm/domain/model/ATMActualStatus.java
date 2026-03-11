package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ATMActualStatus(
    Long id,
    BigDecimal currentBalance,
    LocalDate lastDepositDate,
    LocalDate lastReloadDate,
    Long lastSyncId,
    LocalDate lastTransactionDate,
    LocalDate lastWithdrawalDate,
    LocalDateTime updatedAt,
    AtmData atmData
) {
    
}