package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.OperationalTransactions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.List;
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

        /**
         * Contar transacciones que NO tienen predicción
         */
        @Query("SELECT COUNT(t) FROM OperationalTransactions t " +
                        "WHERE NOT EXISTS (SELECT 1 FROM FraudPredictions fp WHERE fp.transaction = t)")
        long countPendingPredictions();

        /**
         * Obtener transacciones pendientes de predicción (paginado)
         */
        @Query("SELECT t FROM OperationalTransactions t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "LEFT JOIN FETCH c.localization l " +
                        "LEFT JOIN FETCH c.gender g " +
                        "LEFT JOIN FETCH t.category cat " +
                        "WHERE NOT EXISTS (SELECT 1 FROM FraudPredictions fp WHERE fp.transaction = t) " +
                        "ORDER BY t.transDateTime DESC")
        List<OperationalTransactions> findPendingPredictions(Pageable pageable);

        /**
         * Obtener IDs de transacciones pendientes (para procesamiento por lotes)
         */
        @Query("SELECT t.idTransaction FROM OperationalTransactions t " +
                        "WHERE NOT EXISTS (SELECT 1 FROM FraudPredictions fp WHERE fp.transaction = t) " +
                        "ORDER BY t.transDateTime DESC")
        List<Long> findPendingPredictionIds(Pageable pageable);

        /**
         * Contar transacciones por estado
         * Para dashboard: mostrar PENDING / APPROVED / REJECTED
         */
        long countByStatus(String status);
}
