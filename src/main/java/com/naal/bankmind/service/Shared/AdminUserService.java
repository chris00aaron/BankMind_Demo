package com.naal.bankmind.service.Shared;

import com.naal.bankmind.dto.Login.CreateUserRequest;
import com.naal.bankmind.dto.Shared.RoleDto;
import com.naal.bankmind.dto.Shared.UserListDto;
import com.naal.bankmind.entity.Login.Role;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Shared.RoleRepository;
import com.naal.bankmind.repository.Shared.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtener todos los usuarios
     */
    public List<UserListDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserListDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtener todos los roles disponibles
     */
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> RoleDto.builder()
                        .id(role.getIdRole())
                        .codRole(role.getCodRole())
                        .name(role.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Crear un nuevo usuario
     */
    @Transactional
    public UserListDto createUser(CreateUserRequest request) {
        // Validar que no exista el email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        // Validar que no exista el DNI
        if (userRepository.existsByDni(request.getDni())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese DNI");
        }

        // Obtener el rol
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        // Crear usuario
        User user = new User();
        user.setDni(request.getDni());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRol(role);
        user.setEnable(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("✅ Usuario creado: {} ({})", savedUser.getFullName(), savedUser.getEmail());

        return mapToUserListDto(savedUser);
    }

    /**
     * Actualizar un usuario existente
     */
    @Transactional
    public UserListDto updateUser(Long userId, CreateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar email único (excluyendo el usuario actual)
        userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getIdUser().equals(userId))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe otro usuario con ese email");
                });

        // Validar DNI único (excluyendo el usuario actual)
        userRepository.findByDni(request.getDni())
                .filter(u -> !u.getIdUser().equals(userId))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("Ya existe otro usuario con ese DNI");
                });

        // Obtener el rol
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        // Actualizar campos
        user.setDni(request.getDni());
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRol(role);
        user.setUpdatedAt(LocalDateTime.now());

        // Solo actualizar contraseña si se proporciona una nueva
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        log.info("✏️ Usuario actualizado: {} ({})", savedUser.getFullName(), savedUser.getEmail());

        return mapToUserListDto(savedUser);
    }

    /**
     * Habilitar/Deshabilitar usuario
     */
    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        user.setEnable(!user.getEnable());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("🔄 Estado de usuario cambiado: {} - Activo: {}", user.getEmail(), user.getEnable());
    }

    /**
     * Eliminar usuario
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // No permitir eliminar administradores
        if ("ADMIN".equalsIgnoreCase(user.getRol().getCodRole())) {
            throw new IllegalStateException("No se puede eliminar un usuario administrador");
        }

        userRepository.delete(user);
        log.info("🗑️ Usuario eliminado: {} ({})", user.getFullName(), user.getEmail());
    }

    private UserListDto mapToUserListDto(User user) {
        return UserListDto.builder()
                .id(user.getIdUser())
                .dni(user.getDni())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roleCodRole(user.getRol().getCodRole())
                .roleName(user.getRol().getName())
                .enable(user.getEnable())
                .lastAccess(user.getLastAccess())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
