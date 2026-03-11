package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionSummaryDTO(
    LocalDate date,
    BigDecimal withdrawalTotal,
    BigDecimal depositTotal
) {}
