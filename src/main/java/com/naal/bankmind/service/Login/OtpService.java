package com.naal.bankmind.service.Login;

import com.naal.bankmind.entity.Login.OtpVerification;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Login.OtpVerificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de gestión de códigos OTP para autenticación MFA.
 *
 * El envío del código se delega a {@link OtpEmailService},
 * que utiliza el servicio genérico de email con templates Thymeleaf.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final OtpEmailService otpEmailService;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OtpVerification generateOtp(User user) {
        // 1. Invalidar OTPs anteriores del usuario
        otpRepository.invalidateAllUserOtps(user);

        // 2. Generar código de 6 dígitos criptográficamente seguro
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String mfaToken = UUID.randomUUID().toString();

        OtpVerification otp = OtpVerification.builder()
                .user(user)
                .code(code)
                .mfaToken(mfaToken)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .attempts(0)
                .verified(false)
                .build();

        OtpVerification savedOtp = otpRepository.save(otp);

        // 3. Enviar código OTP por email
        log.info("\n---------------------------------------- Generando OTP para usuario ----------------------------------------");
        log.info("Código OTP generado: {}", code);
        log.info("MFA Token generado: {}", mfaToken);
        log.info("Email del usuario: {}", user.getEmail());
        log.info("Tiempo de expiración: {}", otpExpirationMinutes);
        log.info("----------------------------------------------------------------------------------------------------------\n");

        // 4. Enviar código OTP por email
        otpEmailService.sendOtpEmail(user.getEmail(), code, otpExpirationMinutes);

        return savedOtp;
    }

    @Transactional
    public Optional<OtpVerification> verifyOtp(String mfaToken, String code) {
        Optional<OtpVerification> otpOpt = otpRepository.findByMfaTokenAndVerifiedFalse(mfaToken);

        if (otpOpt.isEmpty()) {
            log.warn("MFA Token no encontrado o ya verificado: {}", mfaToken);
            return Optional.empty();
        }

        OtpVerification otp = otpOpt.get();

        if (otp.isExpired()) {
            log.warn("OTP expirado para usuario: {}", otp.getUser().getEmail());
            return Optional.empty();
        }

        if (otp.hasExceededMaxAttempts(maxAttempts)) {
            log.warn("Máximo de intentos excedido para usuario: {}", otp.getUser().getEmail());
            return Optional.empty();
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!otp.getCode().equals(code)) {
            otpRepository.save(otp);
            log.warn("Código OTP incorrecto. Intento {} de {} para usuario: {}",
                    otp.getAttempts(), maxAttempts, otp.getUser().getEmail());
            return Optional.empty();
        }

        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("✅ OTP verificado exitosamente para usuario: {}", otp.getUser().getEmail());
        return Optional.of(otp);
    }

    /**
     * Enmascara el email para mostrar como hint al usuario.
     * Ejemplo: "admin@bankmind.com" → "a***n@bankmind.com"
     */
    public String getEmailHint(String email) {
        return maskEmail(email);
    }

    public int getRemainingAttempts(String mfaToken) {
        return otpRepository.findByMfaTokenAndVerifiedFalse(mfaToken)
                .map(otp -> maxAttempts - otp.getAttempts())
                .orElse(0);
    }

    /**
     * Buscar el usuario asociado a un MFA token activo.
     * Usado para reenvío de OTP.
     */
    public Optional<User> findUserByMfaToken(String mfaToken) {
        return otpRepository.findByMfaTokenAndVerifiedFalse(mfaToken)
                .map(OtpVerification::getUser);
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + parts[1];
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
    }
}
