package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionSummary(
    LocalDate fechaTransaccion,
    BigDecimal retiroTotal,
    BigDecimal depositoTotal
) {}
