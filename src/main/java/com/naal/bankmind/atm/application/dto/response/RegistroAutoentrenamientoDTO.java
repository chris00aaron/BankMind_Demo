package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegistroAutoentrenamientoDTO(
        Long id,
        String nombreModelo,
        LocalDateTime startTraining,
        LocalDateTime endTraining,
        Integer trainingDurationMinutes,
        BigDecimal mae,
        BigDecimal mape,
        BigDecimal rmse,
        @JsonProperty("isProduction") Boolean isProduction) {
}
