package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "marriage")
public class Marriage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marriage")
    private Integer idMarriage;

    @Column(name = "marri_description", length = 100, nullable = false)
    private String marriDescription;
}
