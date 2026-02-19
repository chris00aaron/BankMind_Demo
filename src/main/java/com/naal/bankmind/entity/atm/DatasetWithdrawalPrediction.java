package com.naal.bankmind.entity.atm;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "selfTrainingAudit")
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "dataset_withdrawal_prediction", schema = "public")
public class DatasetWithdrawalPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "count_total", nullable = false)
    private String countTotal;

    @Column(name = "count_train", nullable = false)
    private String countTrain;

    @Column(name = "count_test", nullable = false)
    private String countTest;

    // fecha inicial
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    // fecha final
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> features;

    @Column(length = 100)
    private String target;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonManagedReference("audit-dataset")
    @OneToOne(mappedBy = "dataset", fetch = FetchType.LAZY, optional = false)
    private SelfTrainingAuditWithdrawalModel selfTrainingAudit;
}