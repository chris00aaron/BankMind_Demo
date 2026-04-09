package com.naal.bankmind.entity.Churn;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Registra cada lote de muestreo estratificado para "Inteligencia de Riesgo".
 *
 * DDL:
 *   CREATE TABLE churn_sample_batch (
 *       id                BIGSERIAL PRIMARY KEY,
 *       created_at        TIMESTAMP NOT NULL,
 *       sample_size       INT NOT NULL,
 *       total_customers   BIGINT NOT NULL,
 *       is_active         BOOLEAN NOT NULL DEFAULT FALSE,
 *       triggered_by      VARCHAR(20) NOT NULL
 *   );
 */
@Data
@Entity
@Table(name = "churn_sample_batch")
public class ChurnSampleBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Número de clientes seleccionados en este lote. */
    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    /** Snapshot del total de clientes en BD al momento del muestreo. */
    @Column(name = "total_customers", nullable = false)
    private Long totalCustomers;

    /** Solo un lote puede estar activo a la vez. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    /** 'scheduler' | 'manual' */
    @Column(name = "triggered_by", length = 20, nullable = false)
    private String triggeredBy;
}
