package com.naal.bankmind.atm.application.dto.response;

import java.time.LocalDate;

public record DatasetDetailsDTO(
    Long total,
    Long train,
    Long test,
    LocalDate fechaInicial,
    LocalDate fechaFinal
) {}
