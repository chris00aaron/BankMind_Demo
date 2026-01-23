package com.naal.bankmind.dto.Default.Request;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para solicitar predicción de morosidad.
 * Solo requiere el recordId de la cuenta.
 */
public record PredecirMorosidadRequestDTO(
        @NotNull(message = "El recordId es requerido") Long recordId) {
}
