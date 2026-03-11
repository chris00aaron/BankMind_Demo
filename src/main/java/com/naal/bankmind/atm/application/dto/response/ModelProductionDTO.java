package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ModelProductionDTO(
    String nombreModelo,
    BigDecimal mape,
    BigDecimal mae,
    BigDecimal rmse,
    LocalDate desde
) {}
