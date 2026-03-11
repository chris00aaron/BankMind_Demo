package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.Fraud.FraudClusterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para los perfiles de clustering de fraude.
 *
 * Sigue el patrón de los demás repositorios del módulo.
 * Responsabilidad única: acceso a datos de fraud_cluster_profiles.
 */
@Repository
public interface ClusterProfileRepository extends JpaRepository<FraudClusterProfile, Long> {

    /**
     * Obtiene el último run_date registrado.
     * Usado para recuperar únicamente los perfiles del run más reciente.
     */
    @Query("SELECT MAX(cp.runDate) FROM FraudClusterProfile cp")
    LocalDateTime findLatestRunDate();

    /**
     * Devuelve todos los perfiles del run más reciente, ordenados por fraud_count
     * DESC.
     * El frontend siempre mostrará el análisis más actual.
     */
    List<FraudClusterProfile> findByRunDateOrderByFraudCountDesc(LocalDateTime runDate);

    /**
     * Elimina todos los perfiles de un run específico.
     * Usado por el scheduler para limpiar runs anteriores (retención configurable).
     */
    void deleteByRunDateBefore(LocalDateTime cutoffDate);
}
