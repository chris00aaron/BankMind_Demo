package com.naal.bankmind.atm.infrastructure.mapper;

import com.naal.bankmind.atm.domain.model.ConfidenceModel;
import com.naal.bankmind.entity.atm.WithdrawalModel;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WithdrawalModelMapper {

    public static ConfidenceModel toConfidenceModel(WithdrawalModel withdrawalModel) {
        if (withdrawalModel == null) return null;
        return new ConfidenceModel(
            withdrawalModel.getId(),
            withdrawalModel.getConfidenceLevel(),
            withdrawalModel.getMargin()
        );
    }
}
