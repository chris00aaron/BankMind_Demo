package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegistroAutoentrenamientoDetailsDTO(
        Long codigo,
        String nombreModelo,
        LocalDateTime fechaInicioEntrenamiento,
        LocalDateTime fechaFinEntrenamiento,
        int duracionEntrenamientoMinutos,
        BigDecimal mae,
        BigDecimal mape,
        BigDecimal rmse,
        Map<String, Object> hyperparameters,
        @JsonProperty("isProduction") boolean isProduction,
        DatasetDetailsDTO datasetDetails) {
}
