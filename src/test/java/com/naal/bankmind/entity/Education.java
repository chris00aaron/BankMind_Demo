package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "education")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_education")
    private Integer idEducation;

    @Column(name = "educ_description", length = 100, nullable = false)
    private String educDescription;
}
