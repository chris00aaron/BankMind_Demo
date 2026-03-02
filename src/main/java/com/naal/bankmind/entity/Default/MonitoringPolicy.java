package com.naal.bankmind.entity.Default;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "monitoring_policy")
public class MonitoringPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_monitoring_policy")
    private Long idMonitoringPolicy;

    @Column(name = "policy_name", length = 50)
    private String policyName;

    @Column(name = "psi_threshold", precision = 5, scale = 4)
    private BigDecimal psiThreshold;

    @Column(name = "consecutive_days_trigger")
    private Integer consecutiveDaysTrigger;

    @Column(name = "auc_drop_threshold", precision = 5, scale = 4)
    private BigDecimal aucDropThreshold;

    @Column(name = "ks_drop_threshold", precision = 5, scale = 4)
    private BigDecimal ksDropThreshold;

    @Column(name = "optuna_trials_drift")
    private Integer optunaTrialsDrift;

    @Column(name = "optuna_trials_validation")
    private Integer optunaTrialsValidation;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by", length = 50)
    private String createdBy;
}
