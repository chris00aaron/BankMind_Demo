package com.naal.bankmind.controller;

import com.naal.bankmind.dto.ApiResponse;
import com.naal.bankmind.dto.CreateUserRequest;
import com.naal.bankmind.dto.RoleDto;
import com.naal.bankmind.dto.UserListDto;
import com.naal.bankmind.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

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
    public ResponseEntity<ApiResponse<UserListDto>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            UserListDto user = adminUserService.createUser(request);
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
            @Valid @RequestBody CreateUserRequest request) {
        try {
            UserListDto user = adminUserService.updateUser(id, request);
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
}
