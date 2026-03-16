package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.UltimoEstadoAtmDetailsUseDTO;
import com.naal.bankmind.atm.domain.model.ATMActualStatus;

public class ATMActualStatusMapper {

    public static UltimoEstadoAtmDetailsUseDTO toDto(ATMActualStatus domain) {
        return new UltimoEstadoAtmDetailsUseDTO(
            domain.id(),
            domain.currentBalance(),
            domain.lastDepositDate(),
            domain.lastReloadDate(),
            domain.lastSyncId(),
            domain.lastTransactionDate(),
            domain.lastWithdrawalDate(),
            domain.updatedAt(),
            AtmDataMapper.toDto(domain.atmData())
        );
    }
}
