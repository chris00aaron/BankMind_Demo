package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public interface PSIBinStatsModel {
    BigDecimal getStd();
    BigDecimal getMean();
    BigDecimal getMedian();
    BigDecimal getNullPct();
    Long getNSamples();
}
