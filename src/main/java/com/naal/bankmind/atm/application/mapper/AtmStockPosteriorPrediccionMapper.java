package com.naal.bankmind.atm.application.mapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.naal.bankmind.atm.application.dto.response.SegmentacionRetiroDTO;
import com.naal.bankmind.atm.domain.model.AtmStockPosteriorPrediccion;

@Component
public class AtmStockPosteriorPrediccionMapper {

    public static SegmentacionRetiroDTO toSegmentacionRetiroDTO(List<AtmStockPosteriorPrediccion> newAtmStock) {
        Map<String, BigDecimal> ubicaciones = new HashMap<>();

        for (AtmStockPosteriorPrediccion stock : newAtmStock) {
            BigDecimal valor = ubicaciones.get(stock.tipoUbicacion());
            
            if(valor == null) ubicaciones.put(stock.tipoUbicacion(), stock.retiroPrevisto());
            else ubicaciones.put(stock.tipoUbicacion(), valor.add(stock.retiroPrevisto()));
        }

        return new SegmentacionRetiroDTO(ubicaciones);
    }
}
