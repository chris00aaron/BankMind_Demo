package com.naal.bankmind.atm.application.dto.response;

import java.math.BigDecimal;
import java.util.Map;

public record SegmentacionRetiroDTO(
    Map<String, BigDecimal> ubicaciones
) {}


