package com.naal.bankmind.service.Login.sms;

/**
 * Puerto de abstracción para el envío de SMS.
 *
 * Principio aplicado: Dependency Inversion (DIP).
 * {@link com.naal.bankmind.service.Login.OtpService} depende de esta
 * interfaz, no de Twilio directamente. Si en el futuro se cambia a
 * AWS SNS u otro proveedor, solo se agrega una nueva implementación —
 * OtpService no cambia.
 */
public interface SmsNotificationPort {

    /**
     * Envía el código OTP al número de teléfono indicado.
     *
     * @param toPhoneNumber     Número destino (formato local, ej: "960826691")
     * @param otpCode           Código de 6 dígitos generado
     * @param expirationMinutes Minutos de validez del código
     */
    void sendOtp(String toPhoneNumber, String otpCode, int expirationMinutes);
}
