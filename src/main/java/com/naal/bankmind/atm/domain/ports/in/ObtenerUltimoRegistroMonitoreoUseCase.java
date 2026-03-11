package com.naal.bankmind.atm.domain.ports.in;

import com.naal.bankmind.atm.application.dto.response.PerformanceMonitorModelAtmBaseDTO;

public interface ObtenerUltimoRegistroMonitoreoUseCase {
    /**
     * Busca el ultimo registro del monitoreo posterior a un cargado de datos
     * @return Ultimo Registro de Monitoreo
     */
    PerformanceMonitorModelAtmBaseDTO getLastMonitorin();
}
