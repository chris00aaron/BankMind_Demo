package com.naal.bankmind.atm.domain.model;

import java.util.Map;

public record ProcessLogStep(
    String action,
    String status,
    Map<String, Object> details,
    String timestamp
) {}
