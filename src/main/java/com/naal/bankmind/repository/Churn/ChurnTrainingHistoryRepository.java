package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.ChurnTrainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChurnTrainingHistoryRepository extends JpaRepository<ChurnTrainingHistory, Long> {

    /**
     * Gets the most recent training record by training date.
     * Used as fallback for MLOps metrics when no in-memory training result exists.
     */
    Optional<ChurnTrainingHistory> findTopByOrderByTrainingDateDesc();

    /**
     * Gets the latest model that is currently in production.
     */
    Optional<ChurnTrainingHistory> findTopByInProductionTrueOrderByTrainingDateDesc();
}
