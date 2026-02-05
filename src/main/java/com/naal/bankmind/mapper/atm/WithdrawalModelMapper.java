package com.naal.bankmind.mapper.atm;

import com.naal.bankmind.entity.atm.WithdrawalModel;
import com.naal.bankmind.utils.atm.ConfidenceModel;

import lombok.experimental.UtilityClass;

@UtilityClass
public class WithdrawalModelMapper {

    public static ConfidenceModel toConfidenceModel(WithdrawalModel model) {
        return new ConfidenceModel(
            model.getConfidenceLevel(),
            model.getImportancesFeatures(),
            model.getMargin()
        );
    }
}
