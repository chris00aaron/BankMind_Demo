package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.MonthlyHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MonthlyHistoryRepository extends JpaRepository<MonthlyHistory, Long> {

    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------
    // GENERACIÓN DE DATA SINTETICA

    /**
     * Verifica si ya existe un registro para una cuenta en un período específico.
     * Útil para evitar duplicados al generar data sintética.
     */
    boolean existsByAccountDetails_RecordIdAndMonthlyPeriod(Long recordId, LocalDate monthlyPeriod);

    /**
     * Cuenta registros posteriores a una fecha dada.
     * Útil para verificar cuántos registros sintéticos se generaron.
     */
    long countByMonthlyPeriodAfter(LocalDate date);

    // -----------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    /**
     * Obtiene los últimos 6 meses de historial para una cuenta.
     */
    @Query("SELECT mh FROM MonthlyHistory mh WHERE mh.accountDetails.recordId = :recordId ORDER BY mh.monthlyPeriod DESC")
    List<MonthlyHistory> findTop6ByRecordIdOrderByMonthlyPeriodDesc(@Param("recordId") Long recordId);

    /**
     * Obtiene todo el historial para múltiples cuentas en una sola query.
     * Optimización para evitar N+1 queries en batch.
     */
    @Query("SELECT mh FROM MonthlyHistory mh WHERE mh.accountDetails.recordId IN :recordIds ORDER BY mh.accountDetails.recordId, mh.monthlyPeriod DESC")
    List<MonthlyHistory> findAllByRecordIds(@Param("recordIds") List<Long> recordIds);

    /**
     * Obtiene los últimos N meses de historial de pago para una cuenta.
     * Usar con PageRequest.of(0, 10) para limitar a 10 meses.
     */
    @Query("SELECT mh FROM MonthlyHistory mh WHERE mh.accountDetails.recordId = :recordId ORDER BY mh.monthlyPeriod DESC")
    List<MonthlyHistory> findRecentByRecordId(@Param("recordId") Long recordId, Pageable pageable);
}
