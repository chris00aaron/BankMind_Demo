package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "atms", schema = "public" )
public class Atm {

    @Id
    @Column(name = "id_atm")
    private Long idAtm;

    @Column(name = "max_capacity", precision = 15, scale = 3)
    private BigDecimal maxCapacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_location_type", // Columna en la tabla atms (FK)
            referencedColumnName = "id_location_type", // Columna en la tabla location_type (PK)
            foreignKey = @ForeignKey(name = "fk_atm-location_type"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private LocationType locationType;

    @OneToMany(mappedBy = "atm", fetch = FetchType.LAZY)
    private List<DailyAtmTransaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "atm", fetch = FetchType.LAZY)
    private List<DailyWithdrawalPrediction> predictions = new ArrayList<>();

    @OneToOne(mappedBy = "atm", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AtmCurrentStatus currentStatus;

    private boolean active;
}
