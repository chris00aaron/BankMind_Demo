package com.naal.bankmind.atm.domain.exception;

import java.time.LocalDate;

public class DataInsuficienteParaPredecirException extends RuntimeException {
    
    private static final String MESSAGE = "Actualmente no se cuenta con la suficiente data para generar la prediccion del dia %s";
    
    public DataInsuficienteParaPredecirException(LocalDate fechaParaPredecir) {
        super(String.format(MESSAGE, fechaParaPredecir));
    }
}
