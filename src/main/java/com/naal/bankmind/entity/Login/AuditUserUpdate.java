package com.naal.bankmind.entity.Login;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad de auditoría para registrar actualizaciones de datos de usuario.
 * Tabla independiente sin FK a la tabla user.
 * Cada fila registra un campo modificado con su valor anterior y nuevo
 * (enmascarados).
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_user_update")
public class AuditUserUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_update")
    private Long idAuditUpdate;

    @Column(name = "updated_user_id")
    private Long updatedUserId;

    @Column(name = "updated_user_email", length = 100)
    private String updatedUserEmail;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "admin_email", length = 100)
    private String adminEmail;

    @Column(name = "field_changed", length = 50)
    private String fieldChanged;

    @Column(name = "old_value", length = 255)
    private String oldValue;

    @Column(name = "new_value", length = 255)
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
