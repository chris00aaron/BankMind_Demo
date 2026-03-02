package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;

import com.naal.bankmind.atm.domain.model.PromedioRetiroHistorico;

public interface PromedioRetiroHistoricoRepository {

    /**
     *  Obtener el promedio de retiro historico por cada atm en base al dia y mes
     * @return Lista con el id del atm y su promedio de retiro historico
     */
    List<PromedioRetiroHistorico> obtenerPromediosHistoricos(Short dia,Short mes);
}
