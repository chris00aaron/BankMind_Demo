package com.naal.bankmind.atm.domain.exception;

import com.naal.bankmind.atm.domain.model.FeatureType;

public class AtmFeatureDataTypeException extends IllegalStateException {

    public AtmFeatureDataTypeException(FeatureType featureType, Object value) {
        super(String.format(
            "Feature %s tipo de dato esperado %s pero se obtuvo %s",
            featureType.getKey(),
            featureType.getType().getSimpleName(),
            value.getClass().getSimpleName()
        ));
    }
}

