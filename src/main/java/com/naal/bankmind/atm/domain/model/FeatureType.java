package com.naal.bankmind.atm.domain.model;

public enum FeatureType {
    LAG_1("lag1", "Retiro del día anterior", Integer.class),
    LAG_5("lag5", "Retiro hace 5 días", Integer.class),
    LAG_11("lag11", "Retiro hace 11 días", Integer.class),
    DOMINGO_BAJO("domingo_bajo", "Retiro en domingo", Integer.class),
    CAIDA_RECIENTE("caida_reciente", "Caída reciente en retiros", Integer.class),
    TENDENCIA_LAGS("tendencia_lags", "Tendencia de retiros en lags", Integer.class),
    RATIO_FINDE_VS_SEMANA("ratio_finde_vs_semana", "Ratio de retiros en fin de semana vs semana", Integer.class),
    RETIROS_FINDE_ANTERIOR("retiros_finde_anterior", "Retiros en fin de semana anterior", Integer.class),
    RETIROS_DOMINGO_ANTERIOR("retiros_domingo_anterior", "Retiros en domingo anterior", Integer.class);

    private final String key;
    private final String description;
    private final Class<?> type;

    FeatureType(String key, String description, Class<?> type) {
        this.key = key;
        this.description = description;
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getType() {
        return type;
    }
}
