package com.naal.bankmind.dto.atm.request;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RetiroEfectivoAtmPersonalizadoRequestDTO (    
    LocalDate fechaObjetivo,
    BigDecimal lag1,
    BigDecimal lag5,
    BigDecimal lag11,
    Byte domingoBajo,
    Byte caidaReciente,
    BigDecimal retirosPromedioFinSemana,
    BigDecimal retirosPromedioSemanalSinFinSemana,
    BigDecimal retirosDomingoAnterior,
    BigDecimal retirosPromedioSemanal
){}
