package com.naal.bankmind.service.Login.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación de producción de {@link SmsNotificationPort} usando Twilio.
 *
 * Activa cuando {@code twilio.enabled=true} en application.properties.
 * Inicializa el SDK de Twilio una sola vez en {@link #init()} con las
 * credenciales de la cuenta.
 *
 * El número destino se normaliza con el prefijo de país configurado
 * ({@code twilio.country-prefix}) si aún no lo tiene, por lo que el
 * campo {@code phone} del usuario puede almacenarse sin prefijo (ej:
 * "960826691").
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "twilio.enabled", havingValue = "true")
public class TwilioSmsSender implements SmsNotificationPort {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Value("${twilio.country-prefix:+51}")
    private String countryPrefix;

    /**
     * Inicializa el SDK de Twilio con las credenciales de la cuenta.
     * Se ejecuta una vez al arrancar Spring, antes del primer uso.
     */
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("✅ Twilio SMS inicializado. Número remitente: {}", fromNumber);
    }

    @Override
    public void sendOtp(String toPhoneNumber, String otpCode, int expirationMinutes) {
        String normalizedTo = normalizePhone(toPhoneNumber);

        String messageBody = String.format(
                "🔐 BankMind - Tu código de verificación es: %s%n" +
                        "⏰ Válido por %d minutos.%n" +
                        "No compartas este código con nadie.",
                otpCode, expirationMinutes);

        try {
            Message message = Message.creator(
                    new PhoneNumber(normalizedTo),
                    new PhoneNumber(fromNumber),
                    messageBody).create();

            log.info("📱 SMS OTP enviado vía Twilio. SID: {} | Destino: {}",
                    message.getSid(), maskPhone(normalizedTo));

        } catch (com.twilio.exception.ApiException twilioEx) {
            // El código numérico de error de Twilio (ej: 21211, 21608, 21408)
            // Consultar: https://www.twilio.com/docs/api/errors/<código>
            log.error("❌ Twilio Error [código={}] al enviar a {}: {} | Ver: {}",
                    twilioEx.getCode(),
                    maskPhone(normalizedTo),
                    twilioEx.getMessage(),
                    twilioEx.getMoreInfo());
            throw new SmsDeliveryException(
                    "No se pudo enviar el código de verificación. " +
                            "Verifique que su número de teléfono esté registrado correctamente.",
                    twilioEx);
        } catch (Exception e) {
            log.error("❌ Error inesperado al enviar SMS a {}: {}",
                    maskPhone(normalizedTo), e.getMessage(), e);
            throw new SmsDeliveryException(
                    "No se pudo enviar el código de verificación. " +
                            "Verifique que su número de teléfono esté registrado correctamente.",
                    e);
        }
    }

    /**
     * Añade el prefijo de país si el número aún no lo incluye.
     * Ejemplo: "960826691" → "+51960826691"
     */
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Número de teléfono no configurado para este usuario.");
        }
        String cleaned = phone.trim();
        return cleaned.startsWith("+") ? cleaned : countryPrefix + cleaned;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
