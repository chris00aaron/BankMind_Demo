package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegistroAutoentrenamientoDTO(
    Long id,
    String nombreModelo,
    LocalDateTime startTraining,
    LocalDateTime endTraining,
    Integer trainingDurationMinutes,
    BigDecimal mae,
    BigDecimal mape,
    BigDecimal rmse,
    Boolean isProduction
) {}
