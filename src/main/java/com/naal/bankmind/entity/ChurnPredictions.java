package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "churn_predictions")
public class ChurnPredictions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_churn_prediction")
    private Long idChurnPrediction;

    @ManyToOne
    @JoinColumn(name = "id_customer", nullable = false)
    private Customer customer;

    @Column(name = "prediction_date")
    private LocalDateTime predictionDate;

    @Column(name = "churn_probability", precision = 15, scale = 4)
    private BigDecimal churnProbability;

    @Column(name = "is_churn")
    private Boolean isChurn;

    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    @Column(name = "customer_value", precision = 15, scale = 2)
    private BigDecimal customerValue;

    @Column(name = "prediction_confidence", precision = 5, scale = 4)
    private BigDecimal predictionConfidence;

    @Column(name = "model_version", length = 50)
    private String modelVersion;
}
