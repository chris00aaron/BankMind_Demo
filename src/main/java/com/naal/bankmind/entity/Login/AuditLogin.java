package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad de auditoría para registrar intentos de login (exitosos y fallidos).
 * Tabla independiente sin FK a la tabla user.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_login")
public class AuditLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_login")
    private Long idAuditLogin;

    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "role_name", length = 150)
    private String roleName;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "login_status", length = 20, nullable = false)
    private String loginStatus; // SUCCESS, FAILED

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;
}
