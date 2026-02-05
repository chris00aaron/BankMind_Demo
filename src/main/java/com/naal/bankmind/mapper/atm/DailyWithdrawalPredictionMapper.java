package com.naal.bankmind.mapper.atm;

import com.naal.bankmind.dto.atm.response.RetiroEfectivoAtmPrediccionDTO;
import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DailyWithdrawalPredictionMapper {

    public static RetiroEfectivoAtmPrediccionDTO toRetiroEfectivoAtmPrediccionDTO(DailyWithdrawalPrediction prediction) {
        return new RetiroEfectivoAtmPrediccionDTO(
                prediction.getAtm().getIdAtm(),
                prediction.getPredictedValue(),
                prediction.getLowerBound(),
                prediction.getUpperBound(),
                prediction.getWithdrawalModel().getConfidenceLevel());
    }
}
