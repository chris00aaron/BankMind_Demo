package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record ConfidenceInterval(
    BigDecimal lowerBound,
    BigDecimal upperBound,
    BigDecimal confidenceLevel
) {}
