package com.naal.bankmind.atm.domain.ports.in;

import java.util.List;

import com.naal.bankmind.atm.application.dto.response.EstadoAtmDTO;

public interface ObtenerEstadoActualAtmUseCase {

    /**
     * Obtiene el estado actual de todos los cajeros activos
     * @return Lista de estados actuales de los cajeros activos
     */
    List<EstadoAtmDTO> obtenerEstadoActualAtm();
}
