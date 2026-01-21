package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "horizons_time")
public class HorizonsTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horizon_time")
    private Integer idHorizonTime;

    @Column(name = "label", length = 20)
    private String label;

    @Column(name = "description", length = 50)
    private String description;
}
