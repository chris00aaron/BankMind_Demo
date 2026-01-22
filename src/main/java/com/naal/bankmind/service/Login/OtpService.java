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

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpVerificationRepository otpRepository;

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OtpVerification generateOtp(User user) {
        // Invalidar OTPs anteriores del usuario
        otpRepository.invalidateAllUserOtps(user);

        // Generar código de 6 dígitos
        String code = String.format("%06d", secureRandom.nextInt(1000000));

        // Generar token MFA único
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

        // Simular envío de SMS (en producción usar Twilio, AWS SNS, etc.)
        sendSms(user.getPhone(), code);

        return savedOtp;
    }

    private void sendSms(String phone, String code) {
        // En desarrollo: solo logueamos el código
        // En producción: integrar con servicio de SMS real
        log.info("================================================");
        log.info("📱 SMS ENVIADO (SIMULADO)");
        log.info("📞 Teléfono: {}", maskPhone(phone));
        log.info("🔐 Código OTP: {}", code);
        log.info("⏰ Válido por {} minutos", otpExpirationMinutes);
        log.info("================================================");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    public String getPhoneHint(String phone) {
        return maskPhone(phone);
    }

    @Transactional
    public Optional<OtpVerification> verifyOtp(String mfaToken, String code) {
        Optional<OtpVerification> otpOpt = otpRepository.findByMfaTokenAndVerifiedFalse(mfaToken);

        if (otpOpt.isEmpty()) {
            log.warn("MFA Token no encontrado o ya verificado: {}", mfaToken);
            return Optional.empty();
        }

        OtpVerification otp = otpOpt.get();

        // Verificar si expiró
        if (otp.isExpired()) {
            log.warn("OTP expirado para usuario: {}", otp.getUser().getEmail());
            return Optional.empty();
        }

        // Verificar intentos máximos
        if (otp.hasExceededMaxAttempts(maxAttempts)) {
            log.warn("Máximo de intentos excedido para usuario: {}", otp.getUser().getEmail());
            return Optional.empty();
        }

        // Incrementar intentos
        otp.setAttempts(otp.getAttempts() + 1);

        // Verificar código
        if (!otp.getCode().equals(code)) {
            otpRepository.save(otp);
            log.warn("Código OTP incorrecto. Intento {} de {} para usuario: {}",
                    otp.getAttempts(), maxAttempts, otp.getUser().getEmail());
            return Optional.empty();
        }

        // Código correcto - marcar como verificado
        otp.setVerified(true);
        otpRepository.save(otp);

        log.info("✅ OTP verificado exitosamente para usuario: {}", otp.getUser().getEmail());
        return Optional.of(otp);
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
}
