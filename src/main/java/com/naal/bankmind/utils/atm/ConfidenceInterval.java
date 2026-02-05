package com.naal.bankmind.utils.atm;

import java.math.BigDecimal;

public record ConfidenceInterval(
    BigDecimal lowerBound,
    BigDecimal upperBound,
    BigDecimal confidenceLevel
) {}
