package com.naal.bankmind.atm.domain.ports.in;

import com.naal.bankmind.atm.application.dto.response.ModelProductionDTO;

public interface ObtenerModeloActualEnProduccionUseCase {

    /**
     * Obtiene el modelo actual en producción.
     * @return El modelo actual en producción.
     */
    ModelProductionDTO getModeloActualEnProduccion();
}
