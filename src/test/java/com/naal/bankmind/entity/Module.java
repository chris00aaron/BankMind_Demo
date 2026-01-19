package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "module")
public class Module {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_module")
    private Short idModule;

    @Column(name = "cod_module", length = 100, unique = true)
    private String codModule;

    @Column(name = "name", length = 150)
    private String name;
}
