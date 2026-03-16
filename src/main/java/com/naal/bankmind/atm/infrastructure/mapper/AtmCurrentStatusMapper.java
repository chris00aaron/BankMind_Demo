package com.naal.bankmind.atm.infrastructure.mapper;

import com.naal.bankmind.atm.domain.model.ATMActualStatus;
import com.naal.bankmind.entity.atm.AtmCurrentStatus;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AtmCurrentStatusMapper {
    
    public static ATMActualStatus toDomainWithAtm(AtmCurrentStatus entity) {
        return new ATMActualStatus(
            entity.getIdAtm(),
            entity.getCurrentBalance(),
            entity.getLastDepositDate(),
            entity.getLastReloadDate(),
            entity.getLastSync().getIdSync(),
            entity.getLastTransactionDate(),
            entity.getLastWithdrawalDate(),
            entity.getUpdatedAt(),
            AtmMapper.toDomain(entity.getAtm())
        );
    }
}
