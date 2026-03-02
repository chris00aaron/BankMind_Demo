package com.naal.bankmind.atm.domain.model;

import java.util.Map;

public record ImportanciaCaracteristicasML(
    Map<String, Object> importanciaFeatures
) {}
