package com.naal.bankmind.atm.domain.ports.out.repository;

import java.util.List;

import com.naal.bankmind.atm.domain.model.ATMActualStatus;

public interface ATMActualStatusRepository {

    /**
     * Lista todos los estados actuales de los cajeros
     * @return Lista de estados actuales de los cajeros
     */
    List<ATMActualStatus> listaCurrentStatus();
}
