package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "localization")
public class Localization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localization")
    private Long idLocalization;

    @ManyToOne
    @JoinColumn(name = "id_country")
    private Country country;

    @Column(name = "customer_lat")
    private Double customerLat;

    @Column(name = "customer_long")
    private Double customerLong;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "city_pop")
    private Integer cityPop;
}
