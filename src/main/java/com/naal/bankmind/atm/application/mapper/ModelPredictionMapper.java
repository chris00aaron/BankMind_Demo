package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.ModelProductionDTO;
import com.naal.bankmind.atm.domain.model.ModelPrediction;

public class ModelPredictionMapper {

    public static ModelProductionDTO toModelProductionDTO(ModelPrediction modelPrediction) {
        return new ModelProductionDTO(
            modelPrediction.nombreModelo(),
            modelPrediction.mape(),
            modelPrediction.mae(),
            modelPrediction.rmse(),
            modelPrediction.desde()
        );
    }
}
