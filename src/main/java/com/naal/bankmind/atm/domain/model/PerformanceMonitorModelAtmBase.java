package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record PerformanceMonitorModelAtmBase(
    Long id,
    Map<String, ? extends PSIFeatureResultModel> psiResults,
    BigDecimal mae,
    BigDecimal rmse,
    BigDecimal mape,
    MonitoringDecision decision,
    String message,
    String action,
    Map<String, Object> summary,
    Map<String, Object> detail,
    LocalDateTime createdAt,
    Boolean needSelfTraining
) {}
