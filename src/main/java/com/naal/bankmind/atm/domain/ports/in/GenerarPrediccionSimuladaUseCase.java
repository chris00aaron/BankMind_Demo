package com.naal.bankmind.atm.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;

public interface GenerarPrediccionSimuladaUseCase {

    /**
     * Genera una predicción de retiros de efectivo en cajeros automáticos
     * @param fecha Fecha para la cual se desea generar la predicción
     * @param idWeather ID del clima
     * @return DTO con la predicción de retiros de efectivo en cajeros automáticos
     */
    public List<RetiroEfectivoAtmPrediccion> generarPrediccion(LocalDate fecha, Short idWeather);

}
