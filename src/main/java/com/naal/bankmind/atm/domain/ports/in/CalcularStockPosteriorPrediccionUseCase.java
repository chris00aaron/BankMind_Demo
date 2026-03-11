package com.naal.bankmind.atm.domain.ports.in;

import java.util.List;

import com.naal.bankmind.atm.domain.model.AtmStockPosteriorPrediccion;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;

public interface CalcularStockPosteriorPrediccionUseCase {

    /**
     *  Calcualr Nuevo Stock en base a las predicciones obtenidas
     * @param predicciones son las predicciones de un dia en especifico
     * @return El nuevo estado de stock de los atms
     */
    List<AtmStockPosteriorPrediccion> calcularStockPosteriorRetiro(List<RetiroEfectivoAtmPrediccion> predicciones);
}
