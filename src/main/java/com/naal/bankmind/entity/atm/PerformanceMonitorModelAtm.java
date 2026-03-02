package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString()
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "performance_monitor_model_atm", schema = "public")
public class PerformanceMonitorModelAtm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference("atm-performance-monitor")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_withdrawal_model", // Columna de la tabla dataset_withdrawal_prediction (FK)
            referencedColumnName = "id", // Columna de la tabla dataset_withdrawal_prediction (PK)
            foreignKey = @ForeignKey(name = "fk_performance_monitor_model_atm-withdrawal_model"), 
            nullable = false, // No puede ser null
            unique = true) // Debe ser único
    private WithdrawalModel withdrawalModel;

    @Column(name = "psi_results", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> psiResults;

    @Column(name = "mae", precision = 15, scale = 3 ,nullable = false)
    private BigDecimal mae;

    @Column(name = "rmse",precision = 15, scale = 3 ,nullable = false)
    private BigDecimal rmse;

    @Column(name = "mape", precision = 15, scale = 3 ,nullable = false)
    private BigDecimal mape;

    @Enumerated(EnumType.STRING)
    private MonitoringDecision decision;

    @Column(columnDefinition = "text", nullable = false)
    private String message;

    @Column(columnDefinition = "text", nullable = false)
    private String action;

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> summary;

    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
