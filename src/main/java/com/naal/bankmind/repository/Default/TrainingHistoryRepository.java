package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Default.TrainingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingHistoryRepository extends JpaRepository<TrainingHistory, Long> {
}
