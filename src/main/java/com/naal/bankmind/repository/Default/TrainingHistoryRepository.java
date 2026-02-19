package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.TrainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingHistoryRepository extends JpaRepository<TrainingHistory, Long> {

    @Modifying
    @Query("UPDATE TrainingHistory t SET t.inProduction = false WHERE t.inProduction = true")
    void deactivateAllModels();

    List<TrainingHistory> findAllByOrderByTrainingDateDesc();

    Optional<TrainingHistory> findByInProductionTrue();
}
