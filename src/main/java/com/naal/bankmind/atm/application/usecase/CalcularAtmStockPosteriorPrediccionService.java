package com.naal.bankmind.atm.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.naal.bankmind.atm.domain.model.AtmStock;
import com.naal.bankmind.atm.domain.model.AtmStockPosteriorPrediccion;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.atm.domain.ports.in.CalcularStockPosteriorPrediccionUseCase;
import com.naal.bankmind.atm.domain.ports.out.repository.AtmStockRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor

@Service
public class CalcularAtmStockPosteriorPrediccionService implements CalcularStockPosteriorPrediccionUseCase {

    private final AtmStockRepository atmStockRepository;

    @Override
    public List<AtmStockPosteriorPrediccion> calcularStockPosteriorRetiro(List<RetiroEfectivoAtmPrediccion> predicciones) {

        Map<Long,BigDecimal> retirosPorAtm = predicciones.stream().collect(Collectors.toMap(
            RetiroEfectivoAtmPrediccion::idAtm,
            RetiroEfectivoAtmPrediccion::retiroPrevisto
        ));

        List<AtmStock> atmStockActual = atmStockRepository.obtenerBalanceStockActual();

        return atmStockActual.stream().map(stock -> {
            BigDecimal retiroPrevisto = retirosPorAtm.get(stock.idAtm());
            return AtmStockPosteriorPrediccion.of(stock, retiroPrevisto);
        }).collect(Collectors.toList());
    }
}
