package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Short idRole;

    @Column(name = "cod_role", length = 100, unique = true)
    private String codRole;

    @Column(name = "name", length = 150)
    private String name;
}
