package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public interface PSIFeatureResultModel {
    BigDecimal getPsi();
    String getAlert();
    BigDecimal[] getActualPct();
    BigDecimal[] getExpectedPct();
    BigDecimal getProdSamples();
    BigDecimal getProdNullPct();
}