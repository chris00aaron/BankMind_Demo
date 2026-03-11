package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.DashboardRetiroAtmDTO;
import com.naal.bankmind.atm.application.mapper.AtmDisponibilidadMapper;
import com.naal.bankmind.atm.application.mapper.AtmStockPosteriorPrediccionMapper;
import com.naal.bankmind.atm.application.mapper.RetiroEfectivoAtmPrediccionMapper;
import com.naal.bankmind.atm.domain.model.AtmStockPosteriorPrediccion;
import com.naal.bankmind.atm.domain.ports.in.BuscarRetiroHistoricoUseCase;
import com.naal.bankmind.atm.domain.ports.in.CalcularStockPosteriorPrediccionUseCase;
import com.naal.bankmind.atm.domain.ports.in.GenerarDashboardUseCase;
import com.naal.bankmind.atm.domain.ports.in.PrediccionDiariaUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.AtmDisponibilidadRepository;
import com.naal.bankmind.atm.domain.ports.out.repository.ImportanciaCaracteristicasMLRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class DashboardOrchestrator implements GenerarDashboardUseCase<DashboardRetiroAtmDTO> {

    //Casos de Uso de entrada
	private final PrediccionDiariaUseCase prediccionDiariaUseCase;
	private final CalcularStockPosteriorPrediccionUseCase calcularStockPosteriorPrediccionUseCase;
	private final BuscarRetiroHistoricoUseCase buscarRetiroHistoricoUseCase;
    
    // Estos podrían ser Puertos de salida si no tienen lógica compleja (solo lectura de datos)
    private final ImportanciaCaracteristicasMLRepository importanciaCaracteristicasMLRepository;
    private final AtmDisponibilidadRepository atmDisponibilidadRepository;


    @Override
    public DashboardRetiroAtmDTO generarDashboard() {
        LocalDate fecha = LocalDate.of(2026, 02, 03);

        // 1. Obtención de Datos de Dominio (Pureza)
        var predicciones = prediccionDiariaUseCase.obtenerPrediccionDiaria(fecha);
        var stockPosterior = calcularStockPosteriorPrediccionUseCase.calcularStockPosteriorRetiro(predicciones);
        var disponibilidadAtms = atmDisponibilidadRepository.obtenerDisponibilidadActual();
        var importanciaFeatures = importanciaCaracteristicasMLRepository.obtenerImportanciaCaracteristicasModeloActual();

        // 2. Lógica de Negocio Agregada
        var retirosHistoricos = buscarRetiroHistoricoUseCase.predecirBasadoEnHistoricoComparadoConPrediccion(
            (short) fecha.getDayOfMonth(), (short) fecha.getMonthValue(), predicciones);

        // 3. Mapeo a DTO (La transformación final para React)
        // Nota: He movido el conteo al Mapper o al objeto de respuesta para limpiar el orquestador
        return new DashboardRetiroAtmDTO(
            predicciones.stream().map(RetiroEfectivoAtmPrediccionMapper::toDTO).toList(), 
            RetiroEfectivoAtmPrediccionMapper.toResumenDTO(predicciones), 
            AtmDisponibilidadMapper.toResumenOperativoAtmDTO(disponibilidadAtms),
            retirosHistoricos, 
            AtmStockPosteriorPrediccionMapper.toSegmentacionRetiroDTO(stockPosterior),
            stockPosterior.stream().filter(AtmStockPosteriorPrediccion::tieneRiesgoDeDesabastecimiento).count(),
            importanciaFeatures.importanciaFeatures()
        );
    }
}
