package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.AtmDetailsDTO;
import com.naal.bankmind.atm.domain.model.AtmData;

public class AtmDataMapper {

    public static AtmDetailsDTO toDto(AtmData domain) {
        return new AtmDetailsDTO(
            domain.id(),
            domain.maxCapacity(),
            domain.address(),
            domain.locationType(),
            domain.latitude(),
            domain.longitude(),
            domain.active()
        );
    }
}
