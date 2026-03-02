package com.naal.bankmind.atm.domain.ports.in;

import com.naal.bankmind.atm.application.dto.response.ModelProductionDTO;

public interface ObtenerModeloActualEnProduccionUseCase {

    ModelProductionDTO getModeloActualEnProduccion();
}
