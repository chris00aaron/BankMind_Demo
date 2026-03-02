package com.naal.bankmind.atm.domain.ports.out.repository;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;

public interface RetiroEfectivoAtmPrediccionRepository {

    /**
     * Obtiene las predicciones de retiros de efectivo por fecha.
     * @param fecha Fecha para la cual se desea obtener las predicciones.
     * @return Lista de predicciones de retiros de efectivo.
     */
    List<RetiroEfectivoAtmPrediccion> obtenerPrediccionesPorFecha(LocalDate fecha);

     /**
     * Guarda las predicciones de retiros de efectivo en cajeros automáticos
     * @param outputDataPredictionRetiroAtm Lista de predicciones de retiros de efectivo en cajeros automáticos
     * @return Lista de predicciones de retiros de efectivo en cajeros automáticos
     */
    public List<RetiroEfectivoAtmPrediccion> guardarPredicciones(List<OutputDataPredictionRetiroAtm> outputDataPredictionRetiroAtm);
}
