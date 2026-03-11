package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record AtmData(
    Long id,
    BigDecimal maxCapacity,
    String address,
    String locationType,
    Double latitude,
    Double longitude,
    boolean active
) {}
