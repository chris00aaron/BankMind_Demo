package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.ChurnTrainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Gets the champion (in-production) model with the highest AUC-ROC score.
     * Used to display the best-performing production model metrics in the MLOps dashboard.
     */
    Optional<ChurnTrainingHistory> findTopByInProductionTrueOrderByAucRocDesc();

    /**
     * Gets the 30 most recent training/evaluation records for the evolution chart.
     * Returns in DESC order; caller reverses for chronological display.
     */
    List<ChurnTrainingHistory> findTop30ByOrderByTrainingDateDesc();

    /**
     * Gets the 30 most recent PRODUCTION EVALUATION records (evaluatedSamples IS NOT NULL).
     * Excludes training-run records whose metrics come from a SMOTE-balanced test split
     * and are not comparable to real-world production measurements.
     */
    List<ChurnTrainingHistory> findTop30ByEvaluatedSamplesIsNotNullOrderByTrainingDateDesc();
}
