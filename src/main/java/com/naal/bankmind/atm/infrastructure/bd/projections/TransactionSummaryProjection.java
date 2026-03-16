package com.naal.bankmind.atm.infrastructure.bd.projections;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionSummaryProjection {
    LocalDate getTransactionDate();
    BigDecimal getWithdrawalTotal();
    BigDecimal getDepositTotal();
}
