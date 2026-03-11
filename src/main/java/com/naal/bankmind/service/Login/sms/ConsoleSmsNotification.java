package com.naal.bankmind.service.Login.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación de fallback/desarrollo de {@link SmsNotificationPort}.
 *
 * Activa cuando {@code twilio.enabled=false} (o la propiedad no existe).
 * Imprime el código OTP en el log en lugar de enviarlo por SMS real.
 * Útil para desarrollo local y entornos sin credenciales Twilio.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleSmsNotification implements SmsNotificationPort {

    @Override
    public void sendOtp(String toPhoneNumber, String otpCode, int expirationMinutes) {
        log.info("================================================");
        log.info("📱 SMS SIMULADO (twilio.enabled=false)");
        log.info("📞 Teléfono: {}", maskPhone(toPhoneNumber));
        log.info("🔐 Código OTP: {}", otpCode);
        log.info("⏰ Válido por {} minutos", expirationMinutes);
        log.info("================================================");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
