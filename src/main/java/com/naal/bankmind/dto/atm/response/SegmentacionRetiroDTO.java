package com.naal.bankmind.dto.atm.response;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;

public record SegmentacionRetiroDTO(
    Map<String, BigDecimal> ubicaciones
) {
    
    public static SegmentacionRetiroDTO from(List<DailyWithdrawalPrediction> predictions) {
        Map<String, BigDecimal> ubicaciones = new HashMap<>();

        for (DailyWithdrawalPrediction prediction : predictions) {
            BigDecimal valor = ubicaciones.get(prediction.getAtm().getLocationType().getDescription());
            
            if(valor == null) ubicaciones.put(prediction.getAtm().getLocationType().getDescription(), prediction.getPredictedValue());
            else ubicaciones.put(prediction.getAtm().getLocationType().getDescription(), valor.add(prediction.getPredictedValue()));
        }

        return new SegmentacionRetiroDTO(ubicaciones);
    }
}


