package com.naal.bankmind.service.Login.sms;

/**
 * Excepción lanzada cuando el proveedor de SMS no puede entregar el código OTP.
 *
 * Encapsula errores de red o de la API de Twilio en un tipo de dominio
 * propio, evitando que detalles de infraestructura (clases de Twilio)
 * escapen hacia capas superiores.
 */
public class SmsDeliveryException extends RuntimeException {

    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
