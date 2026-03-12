package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad para tracking de drift por feature individual.
 * Alimenta la gráfica de PSI Evolution en el frontend.
 *
 * Tabla: model_feature_drift
 * Relación: N:1 con fraud_models
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "model_feature_drift", indexes = {
        @Index(name = "idx_drift_model_date", columnList = "id_model, measured_at DESC")
})
public class ModelFeatureDrift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_drift")
    private Long idDrift;

    /**
     * Modelo CHAMPION al que pertenece esta medición de drift.
     * FK hacia fraud_models(id_model).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_model", nullable = false)
    private FraudModels model;

    /**
     * Nombre del feature medido: 'amt', 'city_pop', 'age', 'distance_km', 'hour',
     * 'category', etc.
     */
    @Column(name = "feature_name", nullable = false, length = 50)
    private String featureName;

    /**
     * Valor PSI calculado para este feature en este momento.
     * PSI = Σ (Actual% - Expected%) * ln(Actual% / Expected%)
     *
     * Referencia:
     * < 0.10 → Sin drift (LOW)
     * 0.10-0.25 → Drift moderado (MODERATE) → vigilar
     * > 0.25 → Drift severo (HIGH) → disparar entrenamiento
     */
    @Column(name = "psi_value", nullable = false, precision = 10, scale = 6)
    private BigDecimal psiValue;

    /**
     * Categoría interpretada del PSI: 'LOW', 'MODERATE', 'HIGH'
     */
    @Column(name = "drift_category", length = 20)
    private String driftCategory;

    /**
     * Timestamp de cuándo se midió este drift.
     * Eje X de la gráfica.
     */
    @Column(name = "measured_at", insertable = false, updatable = false)
    private LocalDateTime measuredAt;
}
