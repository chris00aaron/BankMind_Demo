package com.naal.bankmind.atm.infrastructure.mapper;

import com.naal.bankmind.atm.domain.model.AtmData;
import com.naal.bankmind.entity.atm.Atm;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AtmMapper {

    public static AtmData toDomain(Atm entity) {
        return new AtmData(
            entity.getIdAtm(),
            entity.getMaxCapacity(),
            entity.getAddress(),
            entity.getLocationType().getDescription(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.isActive()
        );
    }
}