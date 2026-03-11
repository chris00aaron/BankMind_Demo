package com.naal.bankmind.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.naal.bankmind.atm.domain.exception.SelfTrainingAuditNotFoundException;
import com.naal.bankmind.dto.Shared.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Manejador global de excepciones para la API REST de BankMind.
 * Centraliza las respuestas de error en un formato consistente usando
 * {@link ApiResponse}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de tipo en path variables o request params.
     * Ejemplo: /atm/self-training/{id} recibiendo un valor no numérico como "{id}".
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String receivedValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido";

        String message = String.format(
                "El parámetro '%s' recibió un valor inválido: '%s'. Se esperaba un valor de tipo %s.",
                paramName, receivedValue, expectedType);

        log.warn("MethodArgumentTypeMismatchException — parámetro: '{}', valor recibido: '{}', tipo esperado: {}",
                paramName, receivedValue, expectedType);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Maneja el caso en que un registro de autoentrenamiento no es encontrado por
     * ID.
     */
    @ExceptionHandler(SelfTrainingAuditNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleSelfTrainingAuditNotFound(SelfTrainingAuditNotFoundException ex) {
        log.warn("SelfTrainingAuditNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Catch-all para excepciones no manejadas explícitamente.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException no manejada: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Error interno del servidor. Por favor contacte al administrador."));
    }
}
