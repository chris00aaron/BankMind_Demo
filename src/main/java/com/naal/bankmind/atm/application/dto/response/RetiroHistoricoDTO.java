package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record RetiroHistoricoDTO(
    Long atm,
    BigDecimal retiroHistorico,
    BigDecimal retiroPrevisto
) {}
