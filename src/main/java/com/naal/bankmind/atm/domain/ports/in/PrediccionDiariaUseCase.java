package com.naal.bankmind.atm.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;

public interface PrediccionDiariaUseCase {

    /**
     * Obtiene la prediccion diaria de retiros de efectivo por fecha.
     * @param fecha Fecha para la cual se desea obtener la prediccion.
     * @return Lista de predicciones diarias de retiros de efectivo.
     */
    List<RetiroEfectivoAtmPrediccion> obtenerPrediccionDiaria(LocalDate fecha);
}
