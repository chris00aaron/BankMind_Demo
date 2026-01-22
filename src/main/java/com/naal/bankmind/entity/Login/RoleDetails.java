package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "role_details", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "id_role", "id_module" })
})
public class RoleDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role_detail")
    private Long idRoleDetail;

    @ManyToOne
    @JoinColumn(name = "id_role", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "id_module", nullable = false)
    private Module module;
}
