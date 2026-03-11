package com.naal.bankmind.atm.domain.ports.in;

import java.util.List;

import com.naal.bankmind.atm.application.dto.response.WeatherDTO;

public interface ListarClimasDisponiblesUseCase {
    /**
     * Obtiene la lista de climas disponibles
     * @return Lista de climas disponibles con sus ids y nombres
     */
    List<WeatherDTO> listarClimasDisponibles();
}
