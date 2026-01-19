package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "location_type")
public class LocationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_location_type")
    private Integer idLocationType;

    @Column(name = "description", length = 50, nullable = false)
    private String description;
}
