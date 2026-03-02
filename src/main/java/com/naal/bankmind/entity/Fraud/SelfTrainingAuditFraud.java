package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "self_training_audit_fraud")
public class SelfTrainingAuditFraud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit")
    private Long idAudit;

    @Column(name = "id_dataset")
    private Long idDataset;

    @Column(name = "id_model")
    private Long idModel;

    @Column(name = "id_champion_model")
    private Long idChampionModel;

    @Column(name = "start_training")
    private LocalDateTime startTraining;

    @Column(name = "end_training")
    private LocalDateTime endTraining;

    @Column(name = "training_duration_seconds")
    private Integer trainingDurationSeconds;

    // Métricas del Challenger (Nombres corregidos para coincidir con SQL)
    @Column(name = "accuracy")
    private BigDecimal accuracy;

    @Column(name = "precision_score") // Antes tenías 'precision'
    private BigDecimal precisionScore;

    @Column(name = "recall_score") // Antes tenías 'recall'
    private BigDecimal recallScore;

    @Column(name = "f1_score")
    private BigDecimal f1Score;

    @Column(name = "auc_roc")
    private BigDecimal aucRoc;

    @Column(name = "optimal_threshold")
    private BigDecimal optimalThreshold;

    // Métricas del Champion
    @Column(name = "champion_f1_score")
    private BigDecimal championF1Score;

    @Column(name = "champion_recall")
    private BigDecimal championRecall;

    @Column(name = "champion_auc_roc")
    private BigDecimal championAucRoc;

    // Optuna
    @Column(name = "optuna_best_f1")
    private BigDecimal optunaBestF1;

    @Column(name = "optuna_best_params", columnDefinition = "jsonb")
    private String optunaBestParams;

    // Decision
    @Column(name = "promotion_status")
    private String promotionStatus;

    @Column(name = "promotion_reason")
    private String promotionReason;

    // Trigger & Status
    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "trigger_details", columnDefinition = "jsonb")
    private String triggerDetails;

    @Column(name = "is_success")
    private Boolean isSuccess;

    @Column(name = "error_message")
    private String errorMessage;
}
