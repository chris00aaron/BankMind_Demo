package com.naal.bankmind.atm.infrastructure.mapper;

import java.math.BigDecimal;

import com.naal.bankmind.atm.domain.model.AtmStatusBalance;
import com.naal.bankmind.atm.infrastructure.bd.projections.AtmStatusBalanceProjection;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AtmStatusBalanceProjectionMapper {

    
    public AtmStatusBalance toDomain(AtmStatusBalanceProjection projection) {
        BigDecimal balanceActual = projection.getCurrentBalance().subtract(projection.getPredictedValue());

        return new AtmStatusBalance(
                projection.getIdAtm(),
                projection.getAddress(),
                projection.getLocationType(),
                projection.getMaxCapacity(),
                balanceActual.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balanceActual
            );
    }
}
