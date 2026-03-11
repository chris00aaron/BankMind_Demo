package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RetiroEfectivoAtmPrediccion(
    Long idAtm,
    LocalDate fechaPrediccion,
    BigDecimal retiroPrevisto,
    BigDecimal lowerBound,
    BigDecimal upperBound
) {}
