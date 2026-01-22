package com.naal.bankmind.controller.Login;

import com.naal.bankmind.dto.Login.ApprovePasswordResetRequest;
import com.naal.bankmind.dto.Login.PasswordResetRequestDto;
import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.service.Login.CustomUserDetailsService;
import com.naal.bankmind.service.Login.PasswordResetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/password-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminPasswordController {

    private final PasswordResetService passwordResetService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Listar todas las solicitudes de cambio de contraseña pendientes
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PasswordResetRequestDto>>> getPendingRequests() {
        List<PasswordResetRequestDto> requests = passwordResetService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.success(
                "Solicitudes pendientes obtenidas exitosamente",
                requests));
    }

    /**
     * Listar todas las solicitudes (incluidas procesadas)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PasswordResetRequestDto>>> getAllRequests() {
        List<PasswordResetRequestDto> requests = passwordResetService.getAllRequests();
        return ResponseEntity.ok(ApiResponse.success(
                "Todas las solicitudes obtenidas exitosamente",
                requests));
    }

    /**
     * Aprobar una solicitud de cambio de contraseña
     * La contraseña se resetea a la predeterminada (admin123)
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRequest(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User admin = userDetailsService.findUserByEmail(authentication.getName());
            passwordResetService.approveRequest(id, admin);
            return ResponseEntity.ok(ApiResponse.success(
                    "Solicitud aprobada. La contraseña ha sido reseteada a la predeterminada (admin123)."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Rechazar una solicitud de cambio de contraseña
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRequest request,
            Authentication authentication) {
        try {
            User admin = userDetailsService.findUserByEmail(authentication.getName());
            String notes = request != null ? request.getNotes() : null;
            passwordResetService.rejectRequest(id, notes, admin);
            return ResponseEntity.ok(ApiResponse.success("Solicitud rechazada."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // DTO interno para rechazar
    public static class RejectRequest {
        private String notes;

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
