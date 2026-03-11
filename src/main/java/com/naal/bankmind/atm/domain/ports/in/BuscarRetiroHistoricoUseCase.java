package com.naal.bankmind.atm.domain.ports.in;

import java.util.List;

import com.naal.bankmind.atm.application.dto.response.RetiroHistoricoDTO;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;


public interface BuscarRetiroHistoricoUseCase {

    /**
     * Obtener el retiro historico promedio por cada atm en base al dia y mes, y compararlo 
     * con la prediccion obtenida para ese mismo dia y mes
     * @param diaDelMes Dia del mes para el cual se quiere obtener el retiro historico promedio
     * @param mes Mes para el cual se quiere obtener el retiro historico promedio
     * @param prediccionDeRetirosDTO Prediccion de retiros para el dia y mes especificado, se utiliza para comparar con el retiro historico promedio
     * @return Lista de objetos RetiroHistoricoDTO con los resultados de la comparación
     */
    public List<RetiroHistoricoDTO> predecirBasadoEnHistoricoComparadoConPrediccion(
        Short diaDelMes, 
        Short mes, 
        List<RetiroEfectivoAtmPrediccion> prediccionDeRetirosDTO
    );
}
