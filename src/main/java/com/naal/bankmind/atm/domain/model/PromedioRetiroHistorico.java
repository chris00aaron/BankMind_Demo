package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record PromedioRetiroHistorico(
    Long idAtm,
    BigDecimal retiroPromedio
) {}
