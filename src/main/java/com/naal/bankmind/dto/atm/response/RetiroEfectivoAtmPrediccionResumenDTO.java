package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;
import java.util.List;

public record RetiroEfectivoAtmPrediccionResumenDTO(
    BigDecimal totalRetirosPrevisto,
    BigDecimal totalRetirosPrevistoOptimista,
    BigDecimal totalRetirosPrevistoPesimista
) {

    public static RetiroEfectivoAtmPrediccionResumenDTO from(List<RetiroEfectivoAtmPrediccionDTO> predicciones) {
        BigDecimal totalPrevisto = BigDecimal.ZERO;
        BigDecimal totalOptimista = BigDecimal.ZERO;
        BigDecimal totalPesimista = BigDecimal.ZERO;

        for (RetiroEfectivoAtmPrediccionDTO dto : predicciones) {
            totalPrevisto   = totalPrevisto.add(dto.retiroPrevisto());
            totalOptimista  = totalOptimista.add(dto.upperBound());
            totalPesimista  = totalPesimista.add(dto.lowerBound());
        }
        
        return new RetiroEfectivoAtmPrediccionResumenDTO(totalPrevisto, totalOptimista, totalPesimista); 
    }
}
