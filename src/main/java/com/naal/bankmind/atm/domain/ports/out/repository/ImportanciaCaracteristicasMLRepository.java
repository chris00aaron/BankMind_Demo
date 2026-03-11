package com.naal.bankmind.atm.domain.ports.out.repository;
import com.naal.bankmind.atm.domain.model.ImportanciaCaracteristicasML;

public interface ImportanciaCaracteristicasMLRepository {

    /**
     * Obtener la importancia de las caracteristicas del modelo actual
     * @return Un mapa con el nombre de la caracteristica y su importancia relativa en el modelo
     */
    public ImportanciaCaracteristicasML obtenerImportanciaCaracteristicasModeloActual();
}
