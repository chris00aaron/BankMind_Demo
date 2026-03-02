package com.naal.bankmind.atm.domain.model;

import java.math.BigDecimal;

import com.naal.bankmind.utils.atm.ConfidenceInterval;

public record ConfidenceModel(
    Long idModel,
    BigDecimal confidenceLevel,
    BigDecimal margin
) {

    public ConfidenceInterval calcularIntervaloConfianza(BigDecimal valorPredicho) {
        return new ConfidenceInterval(
            valorPredicho.subtract(margin),
            valorPredicho.add(margin),
            confidenceLevel
        ); 
    }
}
