package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * JPA Entity for the churn_training_history table.
 * This table is written by the Python API (auto_training_service.py)
 * and read by Java for MLOps metrics fallback.
 */
@Data
@Entity
@Table(name = "churn_training_history")
public class ChurnTrainingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_churn_training")
    private Long idChurnTraining;

    @Column(name = "training_date")
    private LocalDateTime trainingDate;

    @Column(name = "trigger_reason", length = 50)
    private String triggerReason;

    @Column(name = "in_production")
    private Boolean inProduction;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    // ── Metrics ──────────────────────────────────────
    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "f1_score")
    private Double f1Score;

    @Column(name = "precision_score")
    private Double precisionScore;

    @Column(name = "recall_score")
    private Double recallScore;

    @Column(name = "auc_roc")
    private Double aucRoc;

    // ── Dataset info ────────────────────────────────
    @Column(name = "train_samples")
    private Integer trainSamples;

    @Column(name = "test_samples")
    private Integer testSamples;

    // ── Monitor evaluation results ──────────────────
    @Column(name = "evaluated_samples")
    private Integer evaluatedSamples;

    @Column(name = "true_positives")
    private Integer truePositives;

    @Column(name = "false_positives")
    private Integer falsePositives;

    @Column(name = "true_negatives")
    private Integer trueNegatives;

    @Column(name = "false_negatives")
    private Integer falseNegatives;

    @Column(name = "recall_threshold")
    private Double recallThreshold;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "warnings", columnDefinition = "jsonb")
    private String warnings; // JSON string written by Python performance_monitor.py
}
