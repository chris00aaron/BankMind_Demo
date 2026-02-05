package com.naal.bankmind.dto.Default.Response;

public record RiskFactorDTO(
                String name, // Nombre de la variable (ej: PAY_0)
                Double impact, // Impacto normalizado (-100 a +100)
                String direction // "positive" = aumenta riesgo, "negative" = reduce riesgo
) {
}
