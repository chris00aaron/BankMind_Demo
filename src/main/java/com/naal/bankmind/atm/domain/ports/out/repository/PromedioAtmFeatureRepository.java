package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;

import com.naal.bankmind.atm.domain.model.PromedioAtmFeature;

public interface PromedioAtmFeatureRepository {

    /**
     * Obtiene los promedios de las features para un ATM en un día y mes específico
     * @param diaDelMes Día del mes
     * @param mesSolicitado Mes
     * @return Lista de promedios de las features para un ATM en un día y mes específico
     */
    List<PromedioAtmFeature> obtenerPromediosAtmFeatures(Short diaDelMes, Short mesSolicitado);
}
