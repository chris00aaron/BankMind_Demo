package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record PSIFeatureResultDTO (
    BigDecimal psi,
    String alert,
    BigDecimal[] actualPct,
    BigDecimal[] expectedPct,
    BigDecimal prodSamples,
    BigDecimal prodNullPct
) {}
