package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.Optional;

import com.naal.bankmind.atm.domain.model.PerformanceMonitorModelAtmBase;

public interface PerformanceMonitorModelAtmBaseRepository {

    /**
     * Obtiene el ultimo registro de monitoreo
     * @return Ultimo Monitoereo
     */
    Optional<PerformanceMonitorModelAtmBase> buscarUltimoRegistroMonitoreo();
}
