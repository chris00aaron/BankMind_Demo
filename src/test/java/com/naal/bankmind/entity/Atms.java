package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "atms")
public class Atms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atm")
    private Long idAtm;

    @ManyToOne
    @JoinColumn(name = "id_location_type")
    private LocationType locationType;

    @Column(name = "max_capacity", precision = 15, scale = 3)
    private BigDecimal maxCapacity;
}
