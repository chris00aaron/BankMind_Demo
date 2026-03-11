package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record AtmStock(
    Long idAtm,
    String tipoUbicacion,
    String ubicacion,
    BigDecimal stock
) {}