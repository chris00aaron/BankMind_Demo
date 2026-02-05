package com.naal.bankmind.dto.atm.request;
import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetiroEfectivoAtmBasadoEnHistoricoRequestDTO (
    @NotNull(message = "La fecha objetivo es obligatoria")
    @FutureOrPresent(message = "La fecha objetivo debe ser una fecha futura o actual")
    LocalDate fechaObjetivo,

    @NotNull(message = "El clima es obligatorio")
    @Positive(message = "El clima debe ser un valor positivo")
    Short idWeather
){
    public Short diaDeLaSemanaSolicitado() {
        return (short) fechaObjetivo.getDayOfWeek().getValue();
    }

    public Short diaDelMesSolicitado() {
        return (short) fechaObjetivo.getDayOfMonth();
    }

    public Short mesSolicitado() {
        return (short) fechaObjetivo.getMonth().getValue();
    }
}
