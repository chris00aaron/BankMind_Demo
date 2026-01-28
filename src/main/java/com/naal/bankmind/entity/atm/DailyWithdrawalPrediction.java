package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "daily_withdrawal_prediction", schema = "public")
public class DailyWithdrawalPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "prediction_date")
    private LocalDate predictionDate;

    @Column(name = "lower_bound", precision = 15, scale = 3)
    private BigDecimal lowerBound;

    @Column(name = "predicted_value", precision = 15, scale = 3)
    private BigDecimal predictedValue;

    @Column(name = "upper_bound", precision = 15, scale = 3)
    private BigDecimal upperBound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atm",
            referencedColumnName = "id_atm", // Columna en la tabla atms (PK)
            foreignKey = @ForeignKey(name = "fk_daily_withdrawal_prediction-atm"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private Atm atm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_withdrawal_model",
            referencedColumnName = "id", // Columna en la tabla withdrawal_models (PK)
            foreignKey = @ForeignKey(name = "fk_daily_withdrawal_prediction-withdrawal_model"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private WithdrawalModel withdrawalModel;
}
