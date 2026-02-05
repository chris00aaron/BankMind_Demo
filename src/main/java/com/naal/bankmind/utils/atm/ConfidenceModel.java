package com.naal.bankmind.utils.atm;

import java.math.BigDecimal;
import java.util.Map;

public record ConfidenceModel(
    BigDecimal confidenceLevel,
    Map<String, Object> importancesFeatures,
    BigDecimal margin
) {}
