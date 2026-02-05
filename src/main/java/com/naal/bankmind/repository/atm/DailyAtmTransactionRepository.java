package com.naal.bankmind.repository.atm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.dto.atm.projection.WithdrawalAvgProjectionDTO;
import com.naal.bankmind.entity.atm.DailyAtmTransaction;
import com.naal.bankmind.entity.atm.TransactionType;

@Repository
public interface DailyAtmTransactionRepository extends JpaRepository<DailyAtmTransaction, Long> {

    @Query(value = "SELECT * FROM fn_withdrawal_avg(:day, :month)", nativeQuery = true)
    List<WithdrawalAvgProjectionDTO> obtenerRetiroDePromedioHistorico(@Param("day") short day, @Param("month") short month);

    @Query("SELECT DISTINCT t FROM DailyAtmTransaction t " +
           "JOIN FETCH t.atm a " +
           "JOIN FETCH a.locationType " +   
           "JOIN FETCH t.weather " +
           "JOIN FETCH t.features " +
           "WHERE t.transactionDate = :fecha AND t.type = :type")
    List<DailyAtmTransaction> obtenerTransaccionesConDetallesParaPrediccion(@Param("fecha") LocalDate fecha, @Param("type") TransactionType type);

    @Query("""
        SELECT t
        FROM DailyAtmTransaction t
        JOIN t.atm a
        WHERE t.transactionDate = :fecha
        AND t.type = :type
        AND a.active = true
        """)
    List<DailyAtmTransaction> findTransaccionesDelDiaEnAtmsActivos(
            @Param("fecha") LocalDate fecha,
            @Param("type") TransactionType type
        );
}
