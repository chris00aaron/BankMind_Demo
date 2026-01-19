package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "gender")
public class Gender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gender")
    private Integer idGender;

    @Column(name = "gender_description", length = 100, nullable = false)
    private String genderDescription;
}
