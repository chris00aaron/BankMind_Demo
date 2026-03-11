package com.naal.bankmind.atm.infrastructure.mapper;

import com.naal.bankmind.atm.domain.model.DatasetDetails;
import com.naal.bankmind.atm.domain.model.RegistroAutoentrenamiento;
import com.naal.bankmind.entity.atm.DatasetWithdrawalPrediction;
import com.naal.bankmind.entity.atm.SelfTrainingAuditWithdrawalModel;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SelfTrainingAuditWithdrawalModelMapper {

    public static RegistroAutoentrenamiento toDomainWithoutDataset(SelfTrainingAuditWithdrawalModel entity) {
        return new RegistroAutoentrenamiento(
                entity.getId(),
                entity.getModelName(),
                entity.getStartTraining(),
                entity.getEndTraining(),
                entity.getTrainingDurationMinutes(),
                entity.getMae(),
                entity.getMape(),
                entity.getRmse(),
                entity.getIsProduction(),
                entity.getHyperparameters()
        );
    }

    public static RegistroAutoentrenamiento toDomainWithDataset(SelfTrainingAuditWithdrawalModel entity) {
        return new RegistroAutoentrenamiento(
                entity.getId(),
                entity.getModelName(),
                entity.getStartTraining(),
                entity.getEndTraining(),
                entity.getTrainingDurationMinutes(),
                entity.getMae(),
                entity.getMape(),
                entity.getRmse(),
                entity.getIsProduction(),
                entity.getHyperparameters(),
                toDatasetDetailsDomain(entity.getDataset())
        );
    }

    private static DatasetDetails toDatasetDetailsDomain(DatasetWithdrawalPrediction dataset) {
        if(dataset == null) {return new DatasetDetails(0L, 0L, 0L, null, null);}
        return new DatasetDetails(
                parseLongSafely(dataset.getCountTotal()),
                parseLongSafely(dataset.getCountTrain()),
                parseLongSafely(dataset.getCountTest()),
                dataset.getStartDate(),
                dataset.getEndDate()
        );
    }


    /**
     * Convierte un String numérico a Long de forma segura.
     * Soporta valores con comas como separador de miles (e.g., "2,500").
     * Devuelve 0 si el valor es nulo, vacío o no es parseable.
     */
    private Long parseLongSafely(String value) {
        if (value == null || value.isBlank()) {return 0L;}
        try {
            return Long.parseLong(value.replace(",", "").trim());
        } catch (NumberFormatException e) { return 0L; }
    }
}
