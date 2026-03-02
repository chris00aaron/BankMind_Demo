package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.MonitoringPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringPolicyRepository extends JpaRepository<MonitoringPolicy, Long> {

    /**
     * Obtiene la política de monitoreo actualmente activa.
     */
    Optional<MonitoringPolicy> findByIsActiveTrue();

    /**
     * Lista todas las políticas ordenadas por fecha de activación.
     */
    List<MonitoringPolicy> findAllByOrderByActivationDateDesc();
}
