package com.naal.bankmind.service.Login;

import com.naal.bankmind.dto.Login.PasswordResetRequestDto;
import com.naal.bankmind.entity.Login.PasswordResetRequest;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Login.PasswordResetRequestRepository;
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
public class PasswordResetService {

    private final PasswordResetRequestRepository passwordResetRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un usuario con ese email"));

        // Verificar si ya existe una solicitud pendiente
        if (passwordResetRepository.existsByUserAndStatus(user, PasswordResetRequest.Status.PENDING)) {
            log.info("Ya existe una solicitud pendiente para: {}", email);
            // No lanzamos error para no revelar información
            return;
        }

        PasswordResetRequest request = PasswordResetRequest.builder()
                .user(user)
                .status(PasswordResetRequest.Status.PENDING)
                .build();

        passwordResetRepository.save(request);
        log.info("📝 Nueva solicitud de cambio de contraseña creada para: {}", email);
    }

    public List<PasswordResetRequestDto> getPendingRequests() {
        return passwordResetRepository
                .findByStatusOrderByRequestedAtDesc(PasswordResetRequest.Status.PENDING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public List<PasswordResetRequestDto> getAllRequests() {
        return passwordResetRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private static final String DEFAULT_PASSWORD = "admin123";

    @Transactional
    public void approveRequest(Long requestId, User processedBy, String ipAddress) {
        PasswordResetRequest request = passwordResetRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (request.getStatus() != PasswordResetRequest.Status.PENDING) {
            throw new IllegalStateException("Esta solicitud ya fue procesada");
        }

        // Actualizar contraseña del usuario a la predeterminada
        User user = request.getUser();
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setMustChangePassword(true); // Marcar que debe cambiar contraseña
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Marcar solicitud como aprobada
        request.setStatus(PasswordResetRequest.Status.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedBy);
        passwordResetRepository.save(request);

        log.info("✅ Contraseña resetada a predeterminada para usuario: {}, Procesado por: {}",
                user.getEmail(), processedBy.getEmail());

        // Registrar en auditoría
        auditService.logUserUpdate(user, processedBy, "password", "[PROTEGIDO]", "[PROTEGIDO]", ipAddress);
    }

    @Transactional
    public void rejectRequest(Long requestId, String notes, User processedBy) {
        PasswordResetRequest request = passwordResetRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (request.getStatus() != PasswordResetRequest.Status.PENDING) {
            throw new IllegalStateException("Esta solicitud ya fue procesada");
        }

        request.setStatus(PasswordResetRequest.Status.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedBy);
        request.setNotes(notes);
        passwordResetRepository.save(request);

        log.info("❌ Solicitud de cambio de contraseña rechazada. Usuario: {}, Motivo: {}",
                request.getUser().getEmail(), notes);
    }

    private PasswordResetRequestDto mapToDto(PasswordResetRequest request) {
        return PasswordResetRequestDto.builder()
                .id(request.getId())
                .userEmail(request.getUser().getEmail())
                .userName(request.getUser().getFullName())
                .userDni(request.getUser().getDni())
                .requestedAt(request.getRequestedAt())
                .status(request.getStatus().name())
                .build();
    }
}
