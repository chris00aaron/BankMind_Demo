package com.naal.bankmind.service.Login;

import com.naal.bankmind.entity.Login.OtpVerification;
import com.naal.bankmind.entity.Login.User;
import com.naal.bankmind.repository.Login.OtpVerificationRepository;
import com.naal.bankmind.service.Login.sms.SmsNotificationPort;

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
 * El envío del SMS se delega a {@link SmsNotificationPort} —
 * no depende directamente de ningún proveedor (Twilio, AWS SNS, etc.).
 * La implementación concreta se inyecta por Spring según la configuración
 * {@code twilio.enabled} en application.properties (DIP).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;
    private final SmsNotificationPort smsNotificationPort;

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

        // 3. Enviar SMS — la implementación real (Twilio) o de consola
        // se resuelve en tiempo de ejecución por Spring (DIP)
        smsNotificationPort.sendOtp(user.getPhone(), code, otpExpirationMinutes);

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

    public String getPhoneHint(String phone) {
        return maskPhone(phone);
    }

    public int getRemainingAttempts(String mfaToken) {
        return otpRepository.findByMfaTokenAndVerifiedFalse(mfaToken)
                .map(otp -> maxAttempts - otp.getAttempts())
                .orElse(0);
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
