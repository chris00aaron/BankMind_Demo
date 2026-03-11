package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record SelfTrainingAudit(
    Long id,
    String modelName,
    LocalDateTime startTraining,
    LocalDateTime endTraining,
    Integer trainingDurationMinutes,
    BigDecimal mae,
    BigDecimal mape,
    BigDecimal rmse,
    BigDecimal marginImprovement,
    Map<String, Object> hyperparameters,
    Map<String, ? extends PSIBinModel> psiBaseline,
    Boolean isProduction
) {}