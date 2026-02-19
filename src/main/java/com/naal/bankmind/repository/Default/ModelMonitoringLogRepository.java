package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.ModelMonitoringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ModelMonitoringLogRepository extends JpaRepository<ModelMonitoringLog, Long> {

    Optional<ModelMonitoringLog> findTopByOrderByIdMonitoringDesc();

    @Query("SELECT m FROM ModelMonitoringLog m WHERE m.monitoringDate = :date ORDER BY m.idMonitoring DESC LIMIT 1")
    Optional<ModelMonitoringLog> findByDate(@Param("date") LocalDate date);

    List<ModelMonitoringLog> findByMonitoringDateBetweenOrderByMonitoringDateAsc(LocalDate start, LocalDate end);

    @Query("SELECT m FROM ModelMonitoringLog m WHERE m.validationStatus = :status ORDER BY m.monitoringDate ASC")
    List<ModelMonitoringLog> findByValidationStatus(@Param("status") String status);
}
