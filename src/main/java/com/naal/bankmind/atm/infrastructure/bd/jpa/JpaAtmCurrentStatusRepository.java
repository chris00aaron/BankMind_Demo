package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.naal.bankmind.entity.atm.AtmCurrentStatus;

public interface JpaAtmCurrentStatusRepository extends JpaRepository<AtmCurrentStatus, Long> {

    /**
     * Lista todos los estados actuales de los cajeros con sus datos completos
     * @return Lista de estados actuales de los cajeros con sus datos completos
     */
    @Query("""
            SELECT acs 
            FROM AtmCurrentStatus acs 
            JOIN FETCH acs.atm 
            JOIN FETCH acs.lastSync 
            ORDER BY acs.idAtm ASC
            """)
    List<AtmCurrentStatus> findAllCompleteWithAtmOrderByIdAtm();
}
