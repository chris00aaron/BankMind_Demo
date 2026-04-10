package com.naal.bankmind.config.Login;

import com.naal.bankmind.entity.Login.Role;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Shared.RoleRepository;
import com.naal.bankmind.repository.Shared.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente que carga datos iniciales de seguridad (roles y usuarios).
 * Se encarga de garantizar que el sistema siempre tenga los accesos necesarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1) // Se ejecuta primero para garantizar que existan roles antes que otros datos
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Definición de roles del sistema
    private static final List<RoleData> ROLES = List.of(
            new RoleData("ADMIN", "Administrador"),
            new RoleData("OPERARIO_MOROSIDAD", "Operario de Morosidad"),
            new RoleData("OPERARIO_ANOMALIAS", "Operario de Anomalías"),
            new RoleData("OPERARIO_DEMANDA_EFECTIVO", "Operario de Demanda de Efectivo"),
            new RoleData("OPERARIO_FUGA_DEMANDA", "Operario de Fuga de Demanda"));

    // Definición de usuarios iniciales
    private static final List<UserData> USERS = List.of(
            new UserData("12345678", "Administrador BankMind", "investigacioncognitech@gmail.com", "admin123",
                    "934658784", "ADMIN", false),
            new UserData("23456789", "Aarón Pérez Gularte", "aaron17650@gmail.com", "123456", "934658784",
                    "OPERARIO_MOROSIDAD", false),
            new UserData("34567890", "Angelo Mejía Ramirez", "angelomejia970@gmail.com", "123456", "960826691",
                    "OPERARIO_ANOMALIAS", false),
            new UserData("45678901", "Juan Chuiz Osorio", "escorpioyvirgo18@gmail.com", "123456", "930723537",
                    "OPERARIO_DEMANDA_EFECTIVO", false),
            new UserData("56789012", "Juan Martínez Vargas", "polociprianouns@gmail.com", "123456", "929055707",
                    "OPERARIO_FUGA_DEMANDA", false));

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🚀 Iniciando carga de datos de seguridad...");
        try {
            seedRoles();
            seedUsers();
            log.info("✅ Carga de seguridad completada.");
        } catch (Exception e) {
            log.error("❌ Error en DataSeeder de Seguridad: {}", e.getMessage());
        }
    }

    private void seedRoles() {
        for (RoleData roleData : ROLES) {
            if (roleRepository.findByCodRole(roleData.codRole()).isEmpty()) {
                Role role = new Role();
                role.setCodRole(roleData.codRole());
                role.setName(roleData.name());
                roleRepository.save(role);
                log.info("📋 Rol creado: {}", roleData.name());
            }
        }
    }

    private void seedUsers() {
        for (UserData userData : USERS) {
            if (!userRepository.existsByEmail(userData.email())) {
                Role role = roleRepository.findByCodRole(userData.roleCodRole())
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + userData.roleCodRole()));

                User user = new User();
                user.setDni(userData.dni());
                user.setFullName(userData.fullName());
                user.setEmail(userData.email());
                user.setPassword(passwordEncoder.encode(userData.password()));
                user.setPhone(userData.phone());
                user.setRol(role);
                user.setEnable(true);
                user.setMustChangePassword(userData.mustChangePassword());
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                userRepository.save(user);
                log.info("👤 Usuario creado: {}", userData.fullName());
            }
        }
    }

    private record RoleData(String codRole, String name) {}
    private record UserData(String dni, String fullName, String email, String password, String phone, String roleCodRole, boolean mustChangePassword) {}
}
