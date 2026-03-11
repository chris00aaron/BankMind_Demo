package com.naal.bankmind.utils;

/**
 * Utilidad para enmascarar datos sensibles en los registros de auditoría.
 * Permite al auditor identificar de qué dato se trata sin exponer el valor
 * completo.
 */
public class DataMaskingUtil {

    private DataMaskingUtil() {
        // Utility class
    }

    /**
     * Enmascara un valor según el nombre del campo.
     * Campos sensibles se enmascaran; campos no sensibles se retornan tal cual.
     */
    public static String mask(String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return switch (fieldName.toLowerCase()) {
            case "email" -> maskEmail(value);
            case "dni" -> maskDni(value);
            case "phone", "telefono" -> maskPhone(value);
            case "fullname", "full_name", "nombre" -> maskName(value);
            default -> value; // Campos no sensibles (role, etc.)
        };
    }

    /**
     * Enmascara un email: "aaron.dev@gmail.com" → "aar***@gmail.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 3) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.substring(0, 3) + "***@" + domain;
    }

    /**
     * Enmascara un DNI: "72345678" → "7234***8"
     */
    public static String maskDni(String dni) {
        if (dni == null || dni.length() < 4) {
            return dni;
        }
        int visibleStart = Math.min(4, dni.length() - 1);
        return dni.substring(0, visibleStart) + "***" + dni.charAt(dni.length() - 1);
    }

    /**
     * Enmascara un teléfono: "987654321" → "987***321"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) {
            return phone;
        }
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 3);
    }

    /**
     * Enmascara un nombre completo: "Aarón López" → "Aar*** Ló***"
     * Cada palabra se enmascara por separado.
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        String[] words = name.split("\\s+");
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0)
                masked.append(" ");
            String word = words[i];
            if (word.length() <= 2) {
                masked.append(word.charAt(0)).append("***");
            } else {
                masked.append(word, 0, Math.min(3, word.length())).append("***");
            }
        }
        return masked.toString();
    }
}
