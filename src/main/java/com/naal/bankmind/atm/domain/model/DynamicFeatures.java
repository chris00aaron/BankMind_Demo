package com.naal.bankmind.atm.domain.model;

import java.util.Map;

import com.naal.bankmind.atm.domain.exception.AtmFeatureDataTypeException;

public class DynamicFeatures {

    private final Map<String, Object> dynamicFeatures;

    public DynamicFeatures() {
        this.dynamicFeatures = null;
    }

    public DynamicFeatures(Map<String, Object> dynamicFeatures) {
        this.dynamicFeatures = dynamicFeatures;
    }

    public static DynamicFeatures fromMap(Map<String, Object> map) {
        return new DynamicFeatures(map);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(FeatureType featureType) {
        Object value = dynamicFeatures.get(featureType.getKey());
        if (value == null) { return null; }

        if (!featureType.getType().isInstance(value)) {
            throw new AtmFeatureDataTypeException(featureType, value);
        }

        return (T) value;
    }

    public Map<String, Object> getDynamicFeatures() {
        return dynamicFeatures;
    }
}


