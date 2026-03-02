package com.naal.bankmind.atm.infrastructure.bd.projections;

import java.math.BigDecimal;

public interface AtmStatusBalanceProjection {

    Long getIdAtm();

    String getLocationType();

    String getAddress();

    BigDecimal getMaxCapacity();

    BigDecimal getCurrentBalance();

    BigDecimal getPredictedValue();
}