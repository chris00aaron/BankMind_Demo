package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "withdrawal_models", schema = "public")
public class WithdrawalModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = true)
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "confidence_level", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "margin", nullable = false, precision = 15, scale = 3)
    private BigDecimal margin;

    @Column(name = "sigma", nullable = false, precision = 15, scale = 3)
    private BigDecimal sigma;

    @Column(name = "t_crit", nullable = false, precision = 6, scale = 4)
    private BigDecimal tCrit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "importances_features", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> importancesFeatures;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_self_training_audit_withdrawal_model", // Columna de la tabla self_training_audit_withdrawal_model (FK)
                referencedColumnName = "id", // Columna de la tabla self_training_audit_withdrawal_model (PK)
                nullable = false, // No puede ser null
                unique = true) // Debe ser único
    private SelfTrainingAuditWithdrawalModel selfTrainingAudit;

    @OneToMany(mappedBy = "withdrawalModel", fetch = FetchType.LAZY)
    private List<DailyWithdrawalPrediction> predictions = new ArrayList<>();
}
