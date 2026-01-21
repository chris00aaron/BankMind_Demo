package com.naal.bankmind.service;

import com.naal.bankmind.dto.PasswordResetRequestDto;
import com.naal.bankmind.entity.PasswordResetRequest;
import com.naal.bankmind.entity.User;
import com.naal.bankmind.repository.PasswordResetRequestRepository;
import com.naal.bankmind.repository.UserRepository;
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

    @Transactional
    public void approveRequest(Long requestId, String newPassword, User processedBy) {
        PasswordResetRequest request = passwordResetRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (request.getStatus() != PasswordResetRequest.Status.PENDING) {
            throw new IllegalStateException("Esta solicitud ya fue procesada");
        }

        // Actualizar contraseña del usuario
        User user = request.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Marcar solicitud como aprobada
        request.setStatus(PasswordResetRequest.Status.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(processedBy);
        passwordResetRepository.save(request);

        log.info("✅ Solicitud de cambio de contraseña aprobada. Usuario: {}, Procesado por: {}",
                user.getEmail(), processedBy.getEmail());
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
