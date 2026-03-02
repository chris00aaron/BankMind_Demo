package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModelPrediction(
    String nombreModelo,
    BigDecimal mape,
    BigDecimal mae,
    BigDecimal rmse,
    LocalDate desde,
    LocalDate hasta
) {}
