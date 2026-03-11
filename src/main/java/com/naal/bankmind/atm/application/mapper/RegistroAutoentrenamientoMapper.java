package com.naal.bankmind.atm.application.mapper;

import java.math.BigDecimal;

import com.naal.bankmind.atm.application.dto.response.DatasetDetailsDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDTO;
import com.naal.bankmind.atm.application.dto.response.RegistroAutoentrenamientoDetailsDTO;
import com.naal.bankmind.atm.domain.model.DatasetDetails;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;

public class RegistroAutoentrenamientoMapper {

    public static RegistroAutoentrenamientoDTO toRegistroAutoentrenamientoDTO(RegistroAutoentrenamiento m) {
        return new RegistroAutoentrenamientoDTO(
                m.getId(),
                m.getModelName(),
                m.getStartTraining(),
                m.getEndTraining(),
                m.getTrainingDurationMinutes(),
                m.getMae(),
                m.getMape().multiply(BigDecimal.valueOf(100)),
                m.getRmse(),
                m.getIsProduction());
    }

    public static RegistroAutoentrenamientoDetailsDTO toRegistroAutoentrenamientoDetailsDTO(
            RegistroAutoentrenamiento m) {
        return new RegistroAutoentrenamientoDetailsDTO(
                m.getId(),
                m.getModelName(),
                m.getStartTraining(),
                m.getEndTraining(),
                m.getTrainingDurationMinutes(),
                m.getMae(),
                m.getMape().multiply(BigDecimal.valueOf(100)),
                m.getRmse(),
                m.getHyperparameters(),
                m.getIsProduction(),
                toDatasetDetailsDTO(m.getDataset()));
    }

    public static DatasetDetailsDTO toDatasetDetailsDTO(DatasetDetails dataset) {
        return new DatasetDetailsDTO(
                dataset.countTotal(),
                dataset.countTrain(),
                dataset.countTest(),
                dataset.startDate(),
                dataset.endDate()
        );
    }
}
