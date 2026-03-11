package com.naal.bankmind.atm.domain.model;

import java.util.List;

public interface PSIBinModel {
    List<Long> getBins();
    PSIBinStatsModel getStats();
    List<Long> getExpectedPct();
}
