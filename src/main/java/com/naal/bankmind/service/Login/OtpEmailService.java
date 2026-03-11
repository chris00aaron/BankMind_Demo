package com.naal.bankmind.service.Login;

import com.naal.bankmind.service.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

/**
 * Servicio para envío de código OTP por email.
 * Usa EmailService genérico para el envío real (mismo que usa FraudEmailService).
 * Responsabilidad: Preparar el contexto y delegar al servicio de email.
 */
@Slf4j
@Service
public class OtpEmailService {

    private final EmailService emailService;

    public OtpEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Enviar código OTP al email del usuario.
     *
     * @param toEmail           Email del destinatario
     * @param otpCode           Código OTP de 6 dígitos
     * @param expirationMinutes Minutos de validez del código
     */
    public void sendOtpEmail(String toEmail, String otpCode, int expirationMinutes) {
        try {
            if (!emailService.isValidEmail(toEmail)) {
                log.warn("Email no válido para envío de OTP: {}", maskEmail(toEmail));
                throw new RuntimeException("El usuario no tiene un email válido configurado para MFA");
            }

            // Preparar contexto para el template
            Context context = new Context();
            context.setVariable("otpCode", otpCode);
            context.setVariable("expirationMinutes", expirationMinutes);

            // Delegar envío al servicio genérico
            String subject = "🔐 BankMind - Código de Verificación";

            emailService.sendTemplatedEmail(
                    toEmail,
                    subject,
                    "email/otp-verification",
                    context);

            log.info("✅ Email OTP enviado a: {}", maskEmail(toEmail));

        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("email válido")) {
                throw e;
            }
            log.error("❌ Error al enviar email OTP a {}: {}", maskEmail(toEmail), e.getMessage(), e);
            throw new RuntimeException("No se pudo enviar el código de verificación por correo.", e);
        }
    }

    /**
     * Enmascara el email para logs.
     * Ejemplo: "admin@bankmind.com" → "a***n@bankmind.com"
     */
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
