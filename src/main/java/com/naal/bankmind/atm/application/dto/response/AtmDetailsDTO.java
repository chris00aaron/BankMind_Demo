package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record AtmDetailsDTO(
    Long id,
    BigDecimal maxCapacity,
    String address,
    String locationType,
    Double latitude,
    Double longitude,
    boolean active
) {}
