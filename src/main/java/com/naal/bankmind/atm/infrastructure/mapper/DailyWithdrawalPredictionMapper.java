package com.naal.bankmind.atm.infrastructure.mapper;

import com.naal.bankmind.atm.domain.model.RetiroEfectivoAtmPrediccion;
import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DailyWithdrawalPredictionMapper {

    public static RetiroEfectivoAtmPrediccion toRetiroEfectivoAtmPrediccion(DailyWithdrawalPrediction dailyWithdrawalPrediction) {
        return new RetiroEfectivoAtmPrediccion(
            dailyWithdrawalPrediction.getAtm().getIdAtm(),
            dailyWithdrawalPrediction.getPredictionDate(),
            dailyWithdrawalPrediction.getPredictedValue(),
            dailyWithdrawalPrediction.getUpperBound(),
            dailyWithdrawalPrediction.getLowerBound()
        );
    }
}
