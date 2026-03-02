package com.naal.bankmind.atm.domain.ports.in;

import java.time.LocalDate;
import java.util.List;

import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;

public interface GenerarPrediccionDiariaUseCase {

    /**
     * Realizar la consulta a la api de prediccion y registrar en la bd el resultado de la predicción
     * @param fecha Fecha para la cual se desea obtener las predicciones
     * @return Lista de predicciones de retiros de efectivo
     */
    List<OutputDataPredictionRetiroAtm> generarPrediccion(LocalDate fecha);
}
