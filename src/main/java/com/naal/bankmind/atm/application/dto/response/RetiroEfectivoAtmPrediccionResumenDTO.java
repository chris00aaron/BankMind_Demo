package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;

public record RetiroEfectivoAtmPrediccionResumenDTO(
    BigDecimal totalRetirosPrevisto,
    BigDecimal totalRetirosPrevistoOptimista,
    BigDecimal totalRetirosPrevistoPesimista
) {}
