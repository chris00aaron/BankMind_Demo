package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad de auditoría para registrar la creación de usuarios por parte del
 * admin.
 * Tabla independiente sin FK a la tabla user.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_user_creation")
public class AuditUserCreation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_creation")
    private Long idAuditCreation;

    @Column(name = "created_user_id")
    private Long createdUserId;

    @Column(name = "created_user_email", length = 100)
    private String createdUserEmail;

    @Column(name = "created_user_role", length = 150)
    private String createdUserRole;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "admin_email", length = 100)
    private String adminEmail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
