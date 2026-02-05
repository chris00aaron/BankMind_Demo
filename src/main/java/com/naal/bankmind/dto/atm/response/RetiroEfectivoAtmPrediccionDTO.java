package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;

public record RetiroEfectivoAtmPrediccionDTO(
    Long idAtm,
    BigDecimal retiroPrevisto,
    BigDecimal lowerBound,
    BigDecimal upperBound,
    BigDecimal confidenceLevel
) {}
