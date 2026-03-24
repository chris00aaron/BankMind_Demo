package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad de auditoría para registrar la desactivación de usuarios por parte
 * del admin.
 * Tabla independiente sin FK a la tabla user.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_user_deactivation")
public class AuditUserDeactivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_deactivation")
    private Long idAuditDeactivation;

    @Column(name = "deactivated_user_id")
    private Long deactivatedUserId;

    @Column(name = "deactivated_user_email", length = 100)
    private String deactivatedUserEmail;

    @Column(name = "deactivated_user_role", length = 150)
    private String deactivatedUserRole;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "admin_email", length = 100)
    private String adminEmail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "deactivated_at", nullable = false)
    private LocalDateTime deactivatedAt;
}
