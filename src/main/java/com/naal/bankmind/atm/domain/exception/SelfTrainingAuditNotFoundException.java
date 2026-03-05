package com.naal.bankmind.atm.domain.exception;

public class SelfTrainingAuditNotFoundException extends RuntimeException {

    public SelfTrainingAuditNotFoundException(Long id) {
        super("Registro de autoentrenamiento no encontrado con id: " + id);
    }
}
