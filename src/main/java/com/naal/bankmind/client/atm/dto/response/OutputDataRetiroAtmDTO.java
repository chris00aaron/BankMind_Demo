package com.naal.bankmind.client.atm.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.naal.bankmind.atm.domain.model.OutputDataPredictionRetiroAtm;

public record OutputDataRetiroAtmDTO( Long atm, BigDecimal retiro, LocalDate prediction_date) {

    //Convertir OutputDataRetiroAtm a OutputDataPredictionRetiroAtm
    public OutputDataPredictionRetiroAtm toOutputDataPredictionRetiroAtm() {
        return new OutputDataPredictionRetiroAtm(atm, retiro, prediction_date);
    }
}
