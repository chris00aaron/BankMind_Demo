package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.naal.bankmind.atm.domain.model.PSIBinModel;

public record SelfTrainingAuditBaseDTO(
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
    Map<String, PSIBinModel> psiBaseline,
    Boolean isProduction
) {}
