package com.naal.bankmind.atm.domain.ports.out.repository;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.InputDataPredictionRetiroAtm;

public interface InputDataPredictionRetiroAtmRepository{

    /**
     * Consulta a la bd la data necesaria para realizar la prediccion
     * @param fecha de la que quiere hacer la predicción
     * @return La data preparada para predicción
     */
    List<InputDataPredictionRetiroAtm> obtenerDataParaRealizarPrediccion(LocalDate fecha);
}
