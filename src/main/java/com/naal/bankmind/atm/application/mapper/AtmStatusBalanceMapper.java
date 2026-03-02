package com.naal.bankmind.atm.application.mapper;

import com.naal.bankmind.atm.application.dto.response.EstadoAtmDTO;
import com.naal.bankmind.atm.domain.model.AtmStatusBalance;

public class AtmStatusBalanceMapper {
    
    public static EstadoAtmDTO toDto(AtmStatusBalance atmStatusBalance) {
        return new EstadoAtmDTO(
                atmStatusBalance.idAtm(),
                atmStatusBalance.direccion(),
                atmStatusBalance.tipoLugar(),
                atmStatusBalance.balanceActual(),
                atmStatusBalance.balanceActualPorcentual(),
                atmStatusBalance.analizaEstado()
        );
    }

}
