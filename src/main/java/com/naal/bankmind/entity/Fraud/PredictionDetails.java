package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "prediction_details")
public class PredictionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detail")
    private Long idDetail;

    @ManyToOne
    @JoinColumn(name = "id_fraud_prediction")
    private FraudPredictions fraudPrediction;

    @Column(name = "feature_name", length = 50)
    private String featureName;

    @Column(name = "feature_value", columnDefinition = "TEXT")
    private String featureValue;

    @Column(name = "shap_value")
    private Float shapValue;

    @Column(name = "risk_description", columnDefinition = "TEXT")
    private String riskDescription;

    @Column(name = "impact_direction", length = 20)
    private String impactDirection;
}
