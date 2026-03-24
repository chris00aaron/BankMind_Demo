package com.naal.bankmind.controller.Login;

import com.naal.bankmind.dto.Login.AuditLoginDTO;
import com.naal.bankmind.dto.Login.AuditUserCreationDTO;
import com.naal.bankmind.dto.Login.AuditUserDeactivationDTO;
import com.naal.bankmind.dto.Login.AuditUserUpdateDTO;
import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.service.Login.AuditService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    /**
     * Obtener todos los registros de auditoría de login
     */
    @GetMapping("/login")
    public ResponseEntity<ApiResponse<List<AuditLoginDTO>>> getLoginAudits() {
        List<AuditLoginDTO> audits = auditService.getAllLoginAudits();
        return ResponseEntity.ok(ApiResponse.success("Auditoría de logins obtenida", audits));
    }

    /**
     * Obtener todos los registros de auditoría de creación de usuarios
     */
    @GetMapping("/user-creation")
    public ResponseEntity<ApiResponse<List<AuditUserCreationDTO>>> getCreationAudits() {
        List<AuditUserCreationDTO> audits = auditService.getAllCreationAudits();
        return ResponseEntity.ok(ApiResponse.success("Auditoría de creación de usuarios obtenida", audits));
    }

    /**
     * Obtener todos los registros de auditoría de actualización de usuarios
     */
    @GetMapping("/user-update")
    public ResponseEntity<ApiResponse<List<AuditUserUpdateDTO>>> getUpdateAudits() {
        List<AuditUserUpdateDTO> audits = auditService.getAllUpdateAudits();
        return ResponseEntity.ok(ApiResponse.success("Auditoría de actualización de usuarios obtenida", audits));
    }

    /**
     * Obtener todos los registros de auditoría de desactivación de usuarios
     */
    @GetMapping("/user-deactivation")
    public ResponseEntity<ApiResponse<List<AuditUserDeactivationDTO>>> getDeactivationAudits() {
        List<AuditUserDeactivationDTO> audits = auditService.getAllDeactivationAudits();
        return ResponseEntity.ok(ApiResponse.success("Auditoría de desactivación de usuarios obtenida", audits));
    }
}
