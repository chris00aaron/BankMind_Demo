package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO con el historial de un ciclo de entrenamiento.
 * Alimenta la tabla "Historial de Entrenamientos" en el Frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingAuditDto {

    @JsonProperty("id_audit")
    private Long idAudit;

    @JsonProperty("id_model")
    private Long idModel;

    @JsonProperty("start_training")
    private LocalDateTime startTraining;

    @JsonProperty("end_training")
    private LocalDateTime endTraining;

    @JsonProperty("training_duration_seconds")
    private Integer trainingDurationSeconds;

    // ---- Métricas del Challenger ----

    @JsonProperty("accuracy")
    private BigDecimal accuracy;

    @JsonProperty("precision_score")
    private BigDecimal precisionScore;

    @JsonProperty("recall_score")
    private BigDecimal recallScore;

    @JsonProperty("f1_score")
    private BigDecimal f1Score;

    @JsonProperty("auc_roc")
    private BigDecimal aucRoc;

    @JsonProperty("optimal_threshold")
    private BigDecimal optimalThreshold;

    // ---- Métricas del Champion al comparar ----

    @JsonProperty("champion_f1_score")
    private BigDecimal championF1Score;

    @JsonProperty("champion_recall")
    private BigDecimal championRecall;

    @JsonProperty("champion_auc_roc")
    private BigDecimal championAucRoc;

    // ---- Optuna ----

    @JsonProperty("optuna_best_f1")
    private BigDecimal optunaBestF1;

    @JsonProperty("optuna_best_params")
    private String optunaBestParams;

    // ---- Decisión ----

    @JsonProperty("promotion_status")
    private String promotionStatus; // PROMOTED | REJECTED | PENDING

    @JsonProperty("promotion_reason")
    private String promotionReason;

    // ---- Trigger & Estado ----

    @JsonProperty("triggered_by")
    private String triggeredBy; // scheduler-java | manual | drift-sensor

    @JsonProperty("is_success")
    private Boolean isSuccess;

    @JsonProperty("error_message")
    private String errorMessage;

    // ---- Dataset usado en este entrenamiento ----

    @JsonProperty("dataset_id")
    private Long datasetId;

    @JsonProperty("dataset_start_date")
    private LocalDateTime datasetStartDate;

    @JsonProperty("dataset_end_date")
    private LocalDateTime datasetEndDate;

    @JsonProperty("dataset_total_samples")
    private Integer datasetTotalSamples;

    @JsonProperty("dataset_count_train")
    private Integer datasetCountTrain;

    @JsonProperty("dataset_count_test")
    private Integer datasetCountTest;

    @JsonProperty("dataset_fraud_ratio")
    private BigDecimal datasetFraudRatio;

    @JsonProperty("dataset_undersampling_ratio")
    private Integer datasetUndersamplingRatio;
}
