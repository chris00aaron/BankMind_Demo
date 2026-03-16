package com.naal.bankmind.atm.application.mapper;

import java.util.HashMap;

import com.naal.bankmind.atm.application.dto.response.SelfTrainingAuditBaseDTO;
import com.naal.bankmind.atm.domain.model.SelfTrainingAudit;

public class SelfTrainingAuditMapper {

    public static SelfTrainingAuditBaseDTO toSelfTrainingAuditBaseDTO(SelfTrainingAudit selfTrainingAudit) {
        return new SelfTrainingAuditBaseDTO(
                selfTrainingAudit.id(),
                selfTrainingAudit.modelName(),
                selfTrainingAudit.startTraining(),
                selfTrainingAudit.endTraining(),
                selfTrainingAudit.trainingDurationMinutes(),
                selfTrainingAudit.mae(),
                selfTrainingAudit.mape(),
                selfTrainingAudit.rmse(),
                selfTrainingAudit.marginImprovement(),
                selfTrainingAudit.hyperparameters(),
                new HashMap<>(selfTrainingAudit.psiBaseline()),
                selfTrainingAudit.isProduction()
        );
    }
}
