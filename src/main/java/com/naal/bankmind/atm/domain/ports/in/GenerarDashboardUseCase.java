package com.naal.bankmind.atm.domain.ports.in;

public interface GenerarDashboardUseCase<DTO> {

    /**
     * Genera un dashboard con la información relevante de los cajeros automáticos, incluyendo:
     * @return DTO con la información del dashboard
     */
    DTO generarDashboard();
}
