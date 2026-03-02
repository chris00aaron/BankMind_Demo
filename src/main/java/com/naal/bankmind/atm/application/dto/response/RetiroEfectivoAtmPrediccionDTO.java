package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record RetiroEfectivoAtmPrediccionDTO(
    Long idAtm,
    BigDecimal retiroPrevisto,
    BigDecimal lowerBound,
    BigDecimal upperBound
) {}
