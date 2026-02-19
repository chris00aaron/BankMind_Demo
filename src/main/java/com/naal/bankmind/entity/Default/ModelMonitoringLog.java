package com.naal.bankmind.entity.Default;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "model_monitoring_log")
public class ModelMonitoringLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_monitoring")
    private Long idMonitoring;

    @Column(name = "monitoring_date", nullable = false)
    private LocalDate monitoringDate = LocalDate.now();

    @Column(name = "id_training_model")
    private Long idTrainingModel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "psi_features", columnDefinition = "jsonb")
    private Map<String, Object> psiFeatures;

    @Column(name = "drift_detected")
    private Boolean driftDetected = false;

    @Column(name = "consecutive_days_drift")
    private Integer consecutiveDaysDrift = 0;

    @Column(name = "validation_status")
    private String validationStatus = "PENDING";

    @Column(name = "auc_roc_real")
    private Double aucRocReal;

    @Column(name = "ks_real")
    private Double ksReal;

    @Column(name = "predicted_default_rate")
    private Double predictedDefaultRate;

    @Column(name = "actual_default_rate")
    private Double actualDefaultRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
