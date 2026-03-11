package com.naal.bankmind.atm.application.usecase;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.PerformanceMonitorModelAtmBaseDTO;
import com.naal.bankmind.atm.application.mapper.PerformanceMonitorModelAtmBaseMapper;
import com.naal.bankmind.atm.domain.ports.in.ObtenerUltimoRegistroMonitoreoUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.PerformanceMonitorModelAtmBaseRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ObtenerUltimoRegistroMonitoreoService implements ObtenerUltimoRegistroMonitoreoUseCase{

    private final PerformanceMonitorModelAtmBaseRepository performanceMonitorModelAtmBaseRepository;

    @Override
    public PerformanceMonitorModelAtmBaseDTO getLastMonitorin() {
        return performanceMonitorModelAtmBaseRepository
                .buscarUltimoRegistroMonitoreo()
                .map(PerformanceMonitorModelAtmBaseMapper::toPerformanceMonitorModelAtmBaseDTO)
                .orElseThrow(() -> new RuntimeException("No se encontró el último registro de monitoreo"));
    }

}
