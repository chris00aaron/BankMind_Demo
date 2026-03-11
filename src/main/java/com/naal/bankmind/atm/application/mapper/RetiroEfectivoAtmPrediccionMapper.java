package com.naal.bankmind.atm.application.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.naal.bankmind.atm.application.dto.response.RetiroEfectivoAtmPrediccionDTO;
import com.naal.bankmind.atm.application.dto.response.RetiroEfectivoAtmPrediccionResumenDTO;
import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;

public class RetiroEfectivoAtmPrediccionMapper {

    public static RetiroEfectivoAtmPrediccionDTO toDTO(RetiroEfectivoAtmPrediccion prediccion) {
        return new RetiroEfectivoAtmPrediccionDTO(
            prediccion.idAtm(),
            prediccion.retiroPrevisto(),
            prediccion.lowerBound(),
            prediccion.upperBound()
        );
    }
    
    public static RetiroEfectivoAtmPrediccionResumenDTO toResumenDTO(List<RetiroEfectivoAtmPrediccion> predicciones) {
        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalOptimista = BigDecimal.ZERO;
        BigDecimal totalPesimista = BigDecimal.ZERO;

        for (RetiroEfectivoAtmPrediccion dto : predicciones) {
            totalPrevisto   = totalPrevisto.add(dto.retiroPrevisto());
            totalOptimista  = totalOptimista.add(dto.upperBound());
            totalPesimista  = totalPesimista.add(dto.lowerBound());
        }
        
        return new RetiroEfectivoAtmPrediccionResumenDTO(totalPrevisto, totalOptimista, totalPesimista); 
    }
}
