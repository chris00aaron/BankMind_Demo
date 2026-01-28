package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.OperationalTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para OperationalTransactions
 */
@Repository
public interface TransactionRepository extends JpaRepository<OperationalTransactions, Long> {

    /**
     * Buscar transacción por ID con datos del cliente cargados
     */
    @Query("SELECT t FROM OperationalTransactions t " +
            "LEFT JOIN FETCH t.creditCard cc " +
            "LEFT JOIN FETCH cc.customer c " +
            "LEFT JOIN FETCH c.localization l " +
            "LEFT JOIN FETCH c.gender g " +
            "WHERE t.idTransaction = :id")
    Optional<OperationalTransactions> findByIdWithCustomerData(Long id);

    /**
     * Buscar transacción por número de transacción
     */
    Optional<OperationalTransactions> findByTransNum(String transNum);
}
