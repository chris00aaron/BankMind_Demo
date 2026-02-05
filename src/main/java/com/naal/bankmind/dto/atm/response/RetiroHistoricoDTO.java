package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;

public record RetiroHistoricoDTO(
    Long atm,
    BigDecimal retiroHistorico,
    BigDecimal retiroPrevisto
) {}
