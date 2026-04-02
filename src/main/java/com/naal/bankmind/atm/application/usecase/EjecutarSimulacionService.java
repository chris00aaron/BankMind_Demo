package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.application.dto.response.PrediccionDeRetirosDTO;
import com.naal.bankmind.atm.application.dto.response.RetiroHistoricoDTO;
import com.naal.bankmind.atm.application.mapper.RetiroEfectivoAtmPrediccionMapper;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.ports.in.BuscarRetiroHistoricoUseCase;
import com.naal.bankmind.atm.domain.ports.in.EjecutarSimulacionUseCase;
import com.naal.bankmind.atm.domain.ports.in.GenerarPrediccionSimuladaUseCase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class EjecutarSimulacionService implements EjecutarSimulacionUseCase {
    private final GenerarPrediccionSimuladaUseCase generarPrediccionSimuladaUseCase;
    private final BuscarRetiroHistoricoUseCase buscarRetiroHistoricoUseCase;

    @Override
    public PrediccionDeRetirosDTO ejecutarSimulacion(LocalDate fecha, Short idWeather) {
       // Generar prediccion
       List<RetiroEfectivoAtmPrediccion> predicciones = generarPrediccionSimuladaUseCase.generarPrediccion(fecha, idWeather);

       //Buscar retiro historico
       Short diaMes = (short) fecha.getDayOfMonth();
       Short mes = (short) fecha.getMonthValue();

       List<RetiroHistoricoDTO> retiroHistorico = buscarRetiroHistoricoUseCase.predecirBasadoEnHistoricoComparadoConPrediccion(diaMes, mes, predicciones);

       return new PrediccionDeRetirosDTO(
        predicciones.stream().map(RetiroEfectivoAtmPrediccionMapper::toDTO).toList()
        , retiroHistorico
        ,RetiroEfectivoAtmPrediccionMapper.toResumenDTO(predicciones));
    }
}