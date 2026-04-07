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

import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@Service
@Transactional
public class PrediccionDiariaService implements PrediccionDiariaUseCase {

    private final RetiroEfectivoAtmPrediccionRepository retiroEfectivoAtmPrediccionRepository;
    private final RealizarPrediccionUseCase realizarPrediccionUseCase;

    // Mapa de locks por fecha para evitar condiciones de carrera en la generacion
    private static final Map<LocalDate, Object> dateLocks = new ConcurrentHashMap<>();

    @Override
    public List<RetiroEfectivoAtmPrediccion> obtenerPrediccionDiaria(LocalDate fecha) {
        // Consultar primero (fast path)
        var predicciones = retiroEfectivoAtmPrediccionRepository.obtenerPrediccionesPorFecha(fecha);
        
        if(predicciones.isEmpty()) {
            // Sincronizar por fecha para asegurar que solo una peticion genere la data
            Object lock = dateLocks.computeIfAbsent(fecha, k -> new Object());
            synchronized(lock) {
                try {
                    // Doble validacion dentro del lock
                    predicciones = retiroEfectivoAtmPrediccionRepository.obtenerPrediccionesPorFecha(fecha);
                    if (predicciones.isEmpty()) {
                        log.info("No se encontraron predicciones para la fecha: {}. Generando...", fecha);
                        List<OutputDataPredictionRetiroAtm> prediccionesGeneradas = realizarPrediccionUseCase.generarPrediccion(fecha);
                        predicciones = retiroEfectivoAtmPrediccionRepository.guardarPredicciones(prediccionesGeneradas);
                        predicciones.forEach(e -> log.info("Se genero la prediccion: {}", e));
                    }
                } finally {
                    // Limpieza opcional del lock para no acumular memoria (en una app real se podria usar un Cache con TTL)
                    dateLocks.remove(fecha);
                }
            }
        } else {
            log.info("Se encontraron {} predicciones para la fecha: {}", predicciones.size(), fecha);
        }
        return predicciones;
    }
}

