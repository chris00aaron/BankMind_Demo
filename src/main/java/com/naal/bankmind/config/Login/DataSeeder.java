package com.naal.bankmind.config.Login;

import com.naal.bankmind.entity.Login.Role;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.entity.RetentionStrategyDef;
import com.naal.bankmind.repository.Churn.RetentionStrategyDefRepository;
import com.naal.bankmind.repository.Shared.RoleRepository;
import com.naal.bankmind.repository.Shared.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Componente que carga datos iniciales (roles y usuarios) al iniciar la
 * aplicación.
 * Solo se ejecuta en el perfil "dev" o "demo" para evitar crear usuarios en
 * producción.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final RetentionStrategyDefRepository retentionStrategyDefRepository;
    private final PasswordEncoder passwordEncoder;

    // Definición de roles del sistema
    private static final List<RoleData> ROLES = List.of(
            new RoleData("ADMIN", "Administrador"),
            new RoleData("OPERARIO_MOROSIDAD", "Operario de Morosidad"),
            new RoleData("OPERARIO_ANOMALIAS", "Operario de Anomalías"),
            new RoleData("OPERARIO_DEMANDA_EFECTIVO", "Operario de Demanda de Efectivo"),
            new RoleData("OPERARIO_FUGA_DEMANDA", "Operario de Fuga de Demanda"));

    // Definición de usuarios iniciales (uno por cada rol)
    // Teléfonos: números reales verificados en Twilio Trial
    private static final List<UserData> USERS = List.of(
            new UserData("12345678", "Administrador BankMind", "admin@bankmind.com", "admin123", "934658784", "ADMIN",
                    false),
            new UserData("23456789", "Ana García López", "morosidad@bankmind.com", "123456", "934658784",
                    "OPERARIO_MOROSIDAD", false),
            new UserData("34567890", "Carlos Pérez Silva", "anomalias@bankmind.com", "123456", "960826691",
                    "OPERARIO_ANOMALIAS", false),
            new UserData("45678901", "María Rodríguez Torres", "demanda@bankmind.com", "123456", "930723537",
                    "OPERARIO_DEMANDA_EFECTIVO", false),
            new UserData("56789012", "Juan Martínez Vargas", "fuga@bankmind.com", "123456", "929055707",
                    "OPERARIO_FUGA_DEMANDA", false));

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🚀 Iniciando carga de datos iniciales...");

        try {
            seedRoles();
            seedUsers();
            seedRetentionStrategies();
            log.info("✅ Carga de datos iniciales completada.");
        } catch (Exception e) {
            log.error("❌ Error durante la carga de datos iniciales: {}", e.getMessage());
            log.error("Stack trace:", e);
        }
    }

    /**
     * Crea estrategias de retención por defecto para el módulo de Fuga.
     */
    private void seedRetentionStrategies() {
        if (retentionStrategyDefRepository.count() == 0) {
            // 1. Descuento
            RetentionStrategyDef s1 = new RetentionStrategyDef();
            s1.setName("Descuento en Comisión");
            s1.setDescription("Reducción del 50% en comisión de mantenimiento por 6 meses.");
            s1.setCostPerClient(new BigDecimal("50.00"));
            s1.setImpactFactor(new BigDecimal("0.35"));
            s1.setIsActive(true);
            s1.setCreatedAt(LocalDateTime.now());
            retentionStrategyDefRepository.save(s1);

            // 2. Cross-Selling
            RetentionStrategyDef s2 = new RetentionStrategyDef();
            s2.setName("Oferta Cross-Selling");
            s2.setDescription("Tasa preferencial en préstamo personal o nueva tarjeta.");
            s2.setCostPerClient(new BigDecimal("20.00"));
            s2.setImpactFactor(new BigDecimal("0.25"));
            s2.setIsActive(true);
            s2.setCreatedAt(LocalDateTime.now());
            retentionStrategyDefRepository.save(s2);

            // 3. VIP
            RetentionStrategyDef s3 = new RetentionStrategyDef();
            s3.setName("Programa VIP Retention");
            s3.setDescription("Acceso a gestor personal y beneficios exclusivos.");
            s3.setCostPerClient(new BigDecimal("200.00"));
            s3.setImpactFactor(new BigDecimal("0.60"));
            s3.setIsActive(true);
            s3.setCreatedAt(LocalDateTime.now());
            retentionStrategyDefRepository.save(s3);

            log.info("📋 Estrategias de retención creadas.");
        }
    }

    /**
     * Crea los roles si no existen
     */
    private void seedRoles() {
        for (RoleData roleData : ROLES) {
            if (roleRepository.findByCodRole(roleData.codRole()).isEmpty()) {
                Role role = new Role();
                role.setCodRole(roleData.codRole());
                role.setName(roleData.name());
                roleRepository.save(role);
                log.info("📋 Rol creado: {} ({})", roleData.name(), roleData.codRole());
            } else {
                log.info("📋 Rol ya existe: {}", roleData.codRole());
            }
        }
    }

    /**
     * Crea los usuarios iniciales si no existen
     */
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
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());

                userRepository.save(user);
                log.info("👤 Usuario creado: {} ({}) - Contraseña: {}",
                        userData.fullName(), userData.email(), userData.password());
            } else {
                log.info("👤 Usuario ya existe: {}", userData.email());
            }
        }
    }

    // Records para datos de configuración
    private record RoleData(String codRole, String name) {
    }

    private record UserData(String dni, String fullName, String email, String password, String phone,
            String roleCodRole, boolean mustChangePassword) {
    }
}
