package com.naal.bankmind.entity.Churn;

import com.naal.bankmind.entity.Customer;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Relación entre un lote de muestra y los clientes seleccionados.
 *
 * DDL:
 *   CREATE TABLE churn_sample_entries (
 *       id          BIGSERIAL PRIMARY KEY,
 *       batch_id    BIGINT NOT NULL REFERENCES churn_sample_batch(id),
 *       customer_id BIGINT NOT NULL REFERENCES customer(id_customer)
 *   );
 *   CREATE INDEX idx_sample_entries_batch ON churn_sample_entries(batch_id);
 */
@Data
@Entity
@Table(name = "churn_sample_entries")
public class ChurnSampleEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ChurnSampleBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
