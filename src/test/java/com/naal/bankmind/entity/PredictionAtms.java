package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "prediction_atms")
public class PredictionAtms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prediction_atms")
    private Long idPredictionAtms;

    @ManyToOne
    @JoinColumn(name = "id_atm")
    private Atms atm;

    @ManyToOne
    @JoinColumn(name = "id_horizon_time")
    private HorizonsTime horizonTime;

    @Column(name = "date_prediction")
    private LocalDateTime datePrediction;

    @Column(name = "objective", length = 50)
    private String objective;

    @Column(name = "value", precision = 15, scale = 3)
    private BigDecimal value;
}
