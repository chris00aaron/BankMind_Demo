package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ATMFeatureModel(
    Long idFeatureStore,
    Long idTransaction,
    LocalDate referenceDate,
    Short dayOfMonth,
    Short dayOfWeek,
    Short month,
    BigDecimal withdrawalAmountDay,
    LocalDateTime createdAt,
    DynamicFeatures dynamicFeatures
) {}