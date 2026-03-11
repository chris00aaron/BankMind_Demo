package com.naal.bankmind.atm.infrastructure.bd.jpa;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.atm.infrastructure.bd.projections.TransactionSummaryProjection;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.TransactionType;

@Repository
public interface JpaDailyAtmTransactionRepository extends JpaRepository<DailyAtmTransaction, Long> {

    @Query("SELECT DISTINCT t FROM DailyAtmTransaction t " +
            "JOIN FETCH t.atm a " +
            "JOIN FETCH a.locationType " +   
            "JOIN FETCH t.weather " +
            "JOIN FETCH t.features " +
            "WHERE t.transactionDate = :fecha AND t.type = :type")
    List<DailyAtmTransaction> obtenerTransaccionesConDetallesParaPrediccion(@Param("fecha") LocalDate fecha, @Param("type") TransactionType type);


    @Query(value = """
        SELECT 
            dat.transaction_date AS transactionDate,
            SUM(CASE WHEN dat.transaction_type = 'WITHDRAWAL' THEN dat.amount ELSE 0 END) AS withdrawalTotal,
            SUM(CASE WHEN dat.transaction_type = 'DEPOSIT' THEN dat.amount ELSE 0 END) AS depositTotal
        FROM daily_atm_transactions dat
        WHERE dat.transaction_date BETWEEN :desde AND :hasta
        GROUP BY dat.transaction_date
        ORDER BY dat.transaction_date
    """, nativeQuery = true)
    List<TransactionSummaryProjection> obtenerResumenTransacciones(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
