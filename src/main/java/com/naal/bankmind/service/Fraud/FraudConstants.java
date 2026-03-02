package com.naal.bankmind.service.Fraud;

/**
 * Constantes de dominio del módulo de Fraude.
 *
 * Regla: cualquier literal de negocio que aparezca en más de una clase
 * vive aquí. Nunca como String suelto en el código.
 */
public final class FraudConstants {

    private FraudConstants() {
        /* No instanciar */ }

    // ── Veredictos de predicción ──────────────────────────────────────────────
    public static final String VEREDICTO_ALTO_RIESGO = "ALTO RIESGO";
    public static final String VEREDICTO_LEGITIMO = "LEGÍTIMO";

    // ── Estados de transacción ────────────────────────────────────────────────
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    // ── Estados de promoción de modelo ────────────────────────────────────────
    public static final String PROMO_PROMOTED = "PROMOTED";
    public static final String PROMO_CHAMPION = "CHAMPION";
    public static final String PROMO_RETIRED = "RETIRED";
    public static final String PROMO_CHALLENGER = "CHALLENGER";

    // ── Escenarios de detección (detection_scenario) ─────────────────────────
    /** Predicción individual (predict/{transactionId} o what-if). */
    public static final int SCENARIO_INDIVIDUAL = 1;
    /** Procesamiento por lotes. */
    public static final int SCENARIO_BATCH = 2;

    // ── Recomendaciones ───────────────────────────────────────────────────────
    public static final String RECOMENDACION_BLOQUEAR = "Bloquear y Notificar";
    public static final String RECOMENDACION_APROBAR = "Aprobar";

    // ── Géneros ───────────────────────────────────────────────────────────────
    public static final String GENDER_FEMALE_CODE = "F";
    public static final String GENDER_MALE_CODE = "M";
}
