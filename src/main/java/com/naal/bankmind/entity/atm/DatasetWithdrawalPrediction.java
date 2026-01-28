package com.naal.bankmind.entity.atm;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
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

    //fecha inicial
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    //fecha final
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToOne(mappedBy = "dataset", fetch = FetchType.LAZY, optional = false)
    private SelfTrainingAuditWithdrawalModel selfTrainingAudit;
}