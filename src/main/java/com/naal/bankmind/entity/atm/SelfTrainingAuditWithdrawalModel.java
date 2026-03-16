package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = { "dataset", "withdrawalModel", "comparedToModel", "comparedModels" })
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "self_training_audit_withdrawal_model", schema = "public")
public class SelfTrainingAuditWithdrawalModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_name", nullable = false, length = 50)
    private String modelName;

    @Column(name = "start_training", nullable = false)
    private LocalDateTime startTraining;

    @Column(name = "end_training", nullable = false)
    private LocalDateTime endTraining;

    @Column(name = "training_duration_minutes", nullable = false)
    private Integer trainingDurationMinutes;

    @Column(name = "mae", nullable = false, precision = 15, scale = 3)
    private BigDecimal mae;

    @Column(name = "mape", nullable = false, precision = 15, scale = 3)
    private BigDecimal mape;

    @Column(name = "rmse", nullable = false, precision = 15, scale = 3)
    private BigDecimal rmse;

    @Column(name = "margin_improvement", nullable = false, precision = 10, scale = 4)
    private BigDecimal marginImprovement;

    @Column(name = "is_production", nullable = false)
    private Boolean isProduction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hyperparameters", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> hyperparameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "psi_baseline", columnDefinition = "jsonb")
    private Map<String, PSIBin> psiBaseline;  

    @JsonBackReference("audit-dataset")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dataset_withdrawal_prediction", // Columna de la tabla dataset_withdrawal_prediction (FK)
            referencedColumnName = "id", // Columna de la tabla dataset_withdrawal_prediction (PK)
            nullable = false, // No puede ser null
            unique = true) // Debe ser único
    private DatasetWithdrawalPrediction dataset;

    @JsonBackReference("audit-model")
    @OneToOne(mappedBy = "selfTrainingAudit", fetch = FetchType.LAZY, optional = false)
    private WithdrawalModel withdrawalModel;

    @JsonBackReference("audit-compared-to-model")
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "compared_to_model", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_self_training_audit_withdrawal_model_compared_to_model"), nullable = true)
    private SelfTrainingAuditWithdrawalModel comparedToModel;

    @JsonBackReference("audit-compared-to-model")
    @OneToMany(mappedBy = "comparedToModel", fetch = FetchType.LAZY)
    private List<SelfTrainingAuditWithdrawalModel> comparedModels;
}