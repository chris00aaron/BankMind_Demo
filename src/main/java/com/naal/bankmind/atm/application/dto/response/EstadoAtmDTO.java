package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record EstadoAtmDTO(
    Long idAtm,
    String direccion,
    String tipoLugar,
    BigDecimal balanceActual,
    BigDecimal porcentaje,
    String estado
) {}