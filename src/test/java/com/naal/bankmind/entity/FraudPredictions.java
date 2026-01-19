package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fraud_predictions")
public class FraudPredictions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fraud_prediction")
    private Long idFraudPrediction;

    @ManyToOne
    @JoinColumn(name = "id_transaction")
    private OperationalTransactions transaction;

    @Column(name = "xgboost_score")
    private Float xgboostScore;

    @Column(name = "ifforest_score")
    private Float ifforestScore;

    @Column(name = "final_decision")
    private Integer finalDecision;

    @Column(name = "veredicto", length = 20)
    private String veredicto;

    @Column(name = "detection_scenario")
    private Integer detectionScenario;

    @Column(name = "prediction_date")
    private LocalDateTime predictionDate;
}
