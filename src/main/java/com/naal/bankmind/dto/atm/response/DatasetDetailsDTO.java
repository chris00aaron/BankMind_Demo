package com.naal.bankmind.dto.atm.response;

import java.time.LocalDate;

public record DatasetDetailsDTO(
    Long total,
    Long train,
    Long test,
    LocalDate fechaInicial,
    LocalDate fechaFinal
) {}
