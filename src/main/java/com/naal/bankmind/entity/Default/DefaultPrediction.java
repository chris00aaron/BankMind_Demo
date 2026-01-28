package com.naal.bankmind.entity.Default;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.naal.bankmind.entity.MonthlyHistory;

@Data
@Entity
@Table(name = "default_prediction")
public class DefaultPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prediction")
    private Long idPrediction;

    @ManyToOne
    @JoinColumn(name = "id_historial")
    private MonthlyHistory monthlyHistory;

    @Column(name = "date_prediction")
    private LocalDateTime datePrediction;

    @Column(name = "default_payment_next_month")
    private Boolean defaultPaymentNextMonth;

    @Column(name = "default_probability", precision = 5, scale = 4)
    private BigDecimal defaultProbability;

    @Column(name = "default_category", length = 50)
    private String defaultCategory;

    @Column(name = "main_risk_factor", length = 255)
    private String mainRiskFactor;

    @ManyToOne
    @JoinColumn(name = "id_production_model")
    private ProductionModelDefault idProductionModel;

    @Column(name = "estimated_loss", precision = 15, scale = 2)
    private BigDecimal estimatedLoss;

    @Column(name = "requesting_user", length = 50)
    private String requestingUser;

    @ManyToOne
    @JoinColumn(name = "id_policy")
    private DefaultPolicies idPolicy;

}
