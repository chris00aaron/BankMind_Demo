package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "clima")
public class Clima {

    @Id
    @Column(name = "id_clima")
    private Short idClima;

    @Column(name = "descripcion", length = 50, nullable = false)
    private String descripcion;

    @Column(name = "impacto", nullable = false)
    private Short impacto;
}
