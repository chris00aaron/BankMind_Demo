package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.Churn.ChurnSampleBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChurnSampleBatchRepository extends JpaRepository<ChurnSampleBatch, Long> {

    Optional<ChurnSampleBatch> findTopByIsActiveTrueOrderByCreatedAtDesc();

    /** Desactiva todos los lotes activos antes de activar el nuevo. */
    @Modifying
    @Query("UPDATE ChurnSampleBatch b SET b.isActive = false WHERE b.isActive = true")
    void deactivateAll();
}
