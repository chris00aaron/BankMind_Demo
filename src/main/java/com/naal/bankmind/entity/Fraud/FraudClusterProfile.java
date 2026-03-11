package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA para los perfiles de clustering de fraude.
 *
 * Cada fila representa UN cluster del ÚLTIMO run de K-Means.
 * La tabla acumula runs históricos (run_date diferencia cada ejecución).
 *
 * Tabla: fraud_cluster_profiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fraud_cluster_profiles", indexes = {
        @Index(name = "idx_cluster_run_date", columnList = "run_date DESC")
})
public class FraudClusterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cluster_profile")
    private Long idClusterProfile;

    /** Timestamp del run de K-Means que generó este perfil. */
    @Column(name = "run_date", nullable = false)
    private LocalDateTime runDate;

    /** Número total de clusters generados en este run. */
    @Column(name = "n_clusters", nullable = false)
    private Integer nClusters;

    /** Identificador interno del cluster (0, 1, 2...). */
    @Column(name = "cluster_id", nullable = false)
    private Integer clusterId;

    /** Cantidad de transacciones fraudulentas en este cluster. */
    @Column(name = "fraud_count", nullable = false)
    private Integer fraudCount;

    /**
     * Porcentaje que representa este cluster sobre el total de fraudes analizados.
     */
    @Column(name = "pct_of_total")
    private Double pctOfTotal;

    /** Monto promedio de las transacciones fraudulentas en este cluster. */
    @Column(name = "avg_amount")
    private Double avgAmount;

    /** Hora promedio (0-23) en que ocurren los fraudes de este cluster. */
    @Column(name = "avg_hour")
    private Double avgHour;

    /** Edad promedio del cliente víctima en este cluster. */
    @Column(name = "avg_age")
    private Double avgAge;

    /** Distancia promedio (km) entre comerciante y cliente en este cluster. */
    @Column(name = "avg_distance_km")
    private Double avgDistanceKm;

    /** Categoría de comercio más frecuente en este cluster (moda). */
    @Column(name = "top_category", length = 100)
    private String topCategory;

    /** Estado geográfico más frecuente en este cluster (moda). */
    @Column(name = "top_state", length = 100)
    private String topState;

    /**
     * Label legible generado automáticamente por el servicio Python.
     * Ejemplo: "Fraude nocturno, alto monto, distancia lejana"
     */
    @Column(name = "label", length = 200)
    private String label;
}
