package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

public record PromedioAtmFeature(
    Long idAtm,
    Integer locationType,
    BigDecimal avgLag1,
    BigDecimal avgLag5,
    BigDecimal avgLag11,
    BigDecimal avgTendenciaLags,
    BigDecimal avgRatioFindeVsSemana,
    BigDecimal avgRetirosFindeAnterior,
    BigDecimal avgRetirosDomingoAnterior,
    Short avgDomingoBajo,
    Short avgCaidaReciente
) {}
