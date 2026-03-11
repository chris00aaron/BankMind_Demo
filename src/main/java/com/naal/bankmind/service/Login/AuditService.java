package com.naal.bankmind.service.Login;

import com.naal.bankmind.dto.Login.AuditLoginDTO;
import com.naal.bankmind.dto.Login.AuditUserCreationDTO;
import com.naal.bankmind.dto.Login.AuditUserUpdateDTO;
import com.naal.bankmind.entity.Login.AuditLogin;
import com.naal.bankmind.entity.Login.AuditUserCreation;
import com.naal.bankmind.entity.Login.AuditUserUpdate;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Login.AuditLoginRepository;
import com.naal.bankmind.repository.Login.AuditUserCreationRepository;
import com.naal.bankmind.repository.Login.AuditUserUpdateRepository;
import com.naal.bankmind.utils.DataMaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de auditoría para registrar eventos de login, creación y
 * actualización de usuarios.
 * Los datos sensibles se almacenan enmascarados usando DataMaskingUtil.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

        private final AuditLoginRepository auditLoginRepository;
        private final AuditUserCreationRepository auditUserCreationRepository;
        private final AuditUserUpdateRepository auditUserUpdateRepository;

        // ──────────────────────────────────────────────────────────────────────────
        // Registro de eventos
        // ──────────────────────────────────────────────────────────────────────────

        /**
         * Registra un intento de login (exitoso o fallido).
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logLogin(User user, String ipAddress, String userAgent, String status, String failureReason) {
                AuditLogin audit = AuditLogin.builder()
                                .idUser(user != null ? user.getIdUser() : null)
                                .email(user != null ? user.getEmail() : null)
                                .roleName(user != null && user.getRol() != null ? user.getRol().getName() : null)
                                .ipAddress(ipAddress)
                                .userAgent(userAgent)
                                .loginStatus(status)
                                .failureReason(failureReason)
                                .loginAt(LocalDateTime.now())
                                .build();

                auditLoginRepository.save(audit);
                log.info("📋 Auditoría Login: {} - Status: {} - IP: {}",
                                user != null ? user.getEmail() : "desconocido", status, ipAddress);
        }

        /**
         * Registra un login fallido cuando no se encuentra el usuario.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logFailedLogin(String email, String ipAddress, String userAgent, String failureReason) {
                AuditLogin audit = AuditLogin.builder()
                                .email(email)
                                .ipAddress(ipAddress)
                                .userAgent(userAgent)
                                .loginStatus("FAILED")
                                .failureReason(failureReason)
                                .loginAt(LocalDateTime.now())
                                .build();

                auditLoginRepository.save(audit);
                log.info("📋 Auditoría Login Fallido: {} - IP: {}", email, ipAddress);
        }

        /**
         * Registra la creación de un nuevo usuario.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logUserCreation(User createdUser, User adminUser, String ipAddress) {
                AuditUserCreation audit = AuditUserCreation.builder()
                                .createdUserId(createdUser.getIdUser())
                                .createdUserEmail(DataMaskingUtil.maskEmail(createdUser.getEmail()))
                                .createdUserRole(createdUser.getRol() != null ? createdUser.getRol().getName() : null)
                                .adminUserId(adminUser.getIdUser())
                                .adminEmail(adminUser.getEmail())
                                .ipAddress(ipAddress)
                                .createdAt(LocalDateTime.now())
                                .build();

                auditUserCreationRepository.save(audit);
                log.info("📋 Auditoría Creación: Admin {} creó usuario {}",
                                adminUser.getEmail(), createdUser.getEmail());
        }

        /**
         * Registra la actualización de un campo de usuario.
         * Los valores se enmascaran según el tipo de campo.
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void logUserUpdate(User updatedUser, User adminUser,
                        String fieldChanged, String oldValue, String newValue,
                        String ipAddress) {
                AuditUserUpdate audit = AuditUserUpdate.builder()
                                .updatedUserId(updatedUser.getIdUser())
                                .updatedUserEmail(DataMaskingUtil.maskEmail(updatedUser.getEmail()))
                                .adminUserId(adminUser.getIdUser())
                                .adminEmail(adminUser.getEmail())
                                .fieldChanged(fieldChanged)
                                .oldValue(DataMaskingUtil.mask(fieldChanged, oldValue))
                                .newValue(DataMaskingUtil.mask(fieldChanged, newValue))
                                .ipAddress(ipAddress)
                                .updatedAt(LocalDateTime.now())
                                .build();

                auditUserUpdateRepository.save(audit);
                log.info("📋 Auditoría Actualización: Admin {} actualizó campo '{}' de usuario {}",
                                adminUser.getEmail(), fieldChanged, updatedUser.getEmail());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Consultas
        // ──────────────────────────────────────────────────────────────────────────

        public List<AuditLoginDTO> getAllLoginAudits() {
                return auditLoginRepository.findAllByOrderByLoginAtDesc().stream()
                                .map(this::mapToLoginDTO)
                                .collect(Collectors.toList());
        }

        public List<AuditUserCreationDTO> getAllCreationAudits() {
                return auditUserCreationRepository.findAllByOrderByCreatedAtDesc().stream()
                                .map(this::mapToCreationDTO)
                                .collect(Collectors.toList());
        }

        public List<AuditUserUpdateDTO> getAllUpdateAudits() {
                return auditUserUpdateRepository.findAllByOrderByUpdatedAtDesc().stream()
                                .map(this::mapToUpdateDTO)
                                .collect(Collectors.toList());
        }

        // ──────────────────────────────────────────────────────────────────────────
        // Mappers
        // ──────────────────────────────────────────────────────────────────────────

        private AuditLoginDTO mapToLoginDTO(AuditLogin entity) {
                return AuditLoginDTO.builder()
                                .id(entity.getIdAuditLogin())
                                .userId(entity.getIdUser())
                                .email(entity.getEmail())
                                .roleName(entity.getRoleName())
                                .ipAddress(entity.getIpAddress())
                                .userAgent(entity.getUserAgent())
                                .loginStatus(entity.getLoginStatus())
                                .failureReason(entity.getFailureReason())
                                .loginAt(entity.getLoginAt())
                                .build();
        }

        private AuditUserCreationDTO mapToCreationDTO(AuditUserCreation entity) {
                return AuditUserCreationDTO.builder()
                                .id(entity.getIdAuditCreation())
                                .createdUserId(entity.getCreatedUserId())
                                .createdUserEmail(entity.getCreatedUserEmail())
                                .createdUserRole(entity.getCreatedUserRole())
                                .adminUserId(entity.getAdminUserId())
                                .adminEmail(entity.getAdminEmail())
                                .ipAddress(entity.getIpAddress())
                                .createdAt(entity.getCreatedAt())
                                .build();
        }

        private AuditUserUpdateDTO mapToUpdateDTO(AuditUserUpdate entity) {
                return AuditUserUpdateDTO.builder()
                                .id(entity.getIdAuditUpdate())
                                .updatedUserId(entity.getUpdatedUserId())
                                .updatedUserEmail(entity.getUpdatedUserEmail())
                                .adminUserId(entity.getAdminUserId())
                                .adminEmail(entity.getAdminEmail())
                                .fieldChanged(entity.getFieldChanged())
                                .oldValue(entity.getOldValue())
                                .newValue(entity.getNewValue())
                                .ipAddress(entity.getIpAddress())
                                .updatedAt(entity.getUpdatedAt())
                                .build();
        }
}
