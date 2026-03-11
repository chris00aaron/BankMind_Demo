package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record PerformanceMonitorModelAtmBaseDTO(
    Long id,
    Map<String, PSIFeatureResultDTO> psiResults,
    BigDecimal mae,
    BigDecimal rmse,
    BigDecimal mape,
    String decision,
    String message,
    String action,
    Map<String, Object> summary,
    Map<String, Object> detail,
    LocalDateTime createdAt,
    Boolean needSelfTraining
) {}
