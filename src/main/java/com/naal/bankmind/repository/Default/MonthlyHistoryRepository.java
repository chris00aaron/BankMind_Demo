package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.MonthlyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyHistoryRepository extends JpaRepository<MonthlyHistory, Long> {

    /**
     * Obtiene los últimos 6 meses de historial para una cuenta, ordenados por fecha
     * descendente.
     */
    @Query("SELECT mh FROM MonthlyHistory mh WHERE mh.accountDetails.recordId = :recordId ORDER BY mh.monthlyPeriod DESC")
    List<MonthlyHistory> findTop6ByRecordIdOrderByMonthlyPeriodDesc(@Param("recordId") Long recordId);
}
