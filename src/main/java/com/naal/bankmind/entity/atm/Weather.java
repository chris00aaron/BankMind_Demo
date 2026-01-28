package com.naal.bankmind.entity.atm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "weathers", schema = "public")
public class Weather {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_weather")
    private Short idWeather;

    @Column(name = "description", length = 50, nullable = false)
    private String description;

    @Column(name = "impact", nullable = false)
    private Short impact;

    @OneToMany(mappedBy = "weather", fetch = FetchType.LAZY)
    private List<DailyAtmTransaction> transactions = new ArrayList<>();
}
