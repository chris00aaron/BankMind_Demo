package com.naal.bankmind.controller.Login;

import com.naal.bankmind.dto.Login.CreateUserRequest;
import com.naal.bankmind.dto.Login.UpdateUserRequest;
import com.naal.bankmind.dto.Shared.ApiResponse;
import com.naal.bankmind.dto.Shared.RoleDto;
import com.naal.bankmind.dto.Shared.UserListDto;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.service.Login.CustomUserDetailsService;
import com.naal.bankmind.service.Shared.AdminUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * Listar todos los usuarios
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserListDto>>> getAllUsers() {
        List<UserListDto> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Usuarios obtenidos exitosamente", users));
    }

    /**
     * Listar todos los roles disponibles
     */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        List<RoleDto> roles = adminUserService.getAllRoles();
        return ResponseEntity.ok(ApiResponse.success("Roles obtenidos exitosamente", roles));
    }

    /**
     * Crear nuevo usuario
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserListDto>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            User adminUser = userDetailsService.findUserByEmail(authentication.getName());
            String ipAddress = getClientIp(httpRequest);
            UserListDto user = adminUserService.createUser(request, adminUser, ipAddress);
            return ResponseEntity.ok(ApiResponse.success("Usuario creado exitosamente", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Actualizar usuario existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserListDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            User adminUser = userDetailsService.findUserByEmail(authentication.getName());
            String ipAddress = getClientIp(httpRequest);
            UserListDto user = adminUserService.updateUser(id, request, adminUser, ipAddress);
            return ResponseEntity.ok(ApiResponse.success("Usuario actualizado exitosamente", user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Cambiar estado de usuario (habilitar/deshabilitar)
     */
    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        try {
            adminUserService.toggleUserStatus(id);
            return ResponseEntity.ok(ApiResponse.success("Estado de usuario actualizado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Eliminar usuario
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            adminUserService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("Usuario eliminado exitosamente"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
