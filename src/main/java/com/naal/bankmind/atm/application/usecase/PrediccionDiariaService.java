package com.naal.bankmind.atm.application.usecase;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.ports.in.PrediccionDiariaUseCase;
import com.naal.bankmind.atm.domain.ports.in.RealizarPrediccionUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.RetiroEfectivoAtmPrediccionRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor

@Service
public class PrediccionDiariaService implements PrediccionDiariaUseCase {

    private final RetiroEfectivoAtmPrediccionRepository retiroEfectivoAtmPrediccionRepository;

    private final RealizarPrediccionUseCase realizarPrediccionUseCase;

    @Override
    public List<RetiroEfectivoAtmPrediccion> obtenerPrediccionDiaria(LocalDate fecha) {

        var predicciones = retiroEfectivoAtmPrediccionRepository.obtenerPrediccionesPorFecha(fecha);
        
        if(predicciones.isEmpty()) {
            log.info("No se encontraron predicciones para la fecha: {}", fecha);
            List<OutputDataPredictionRetiroAtm> prediccionesGeneradas = realizarPrediccionUseCase.generarPrediccion(fecha);
            predicciones = retiroEfectivoAtmPrediccionRepository.guardarPredicciones(prediccionesGeneradas);
            predicciones.forEach(e -> log.info("Se genero la prediccion: {}", e));
        }else{
            log.info("Se encontraron {} predicciones para la fecha: {}", predicciones.size(), fecha);
        }
        return predicciones;
    }
}
