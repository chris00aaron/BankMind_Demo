package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

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
    boolean isProduction,
    DatasetDetailsDTO datasetDetails
) {}
