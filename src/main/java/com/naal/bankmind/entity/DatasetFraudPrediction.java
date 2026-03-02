package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "dataset_fraud_prediction")
public class DatasetFraudPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dataset")
    private Long idDataset;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_samples")
    private Integer totalSamples;

    @Column(name = "count_train")
    private Integer countTrain;

    @Column(name = "count_test")
    private Integer countTest;

    @Column(name = "fraud_ratio")
    private BigDecimal fraudRatio;

    @Column(name = "undersampling_ratio")
    private Integer undersamplingRatio;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
