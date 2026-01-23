package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.DefaultPrediction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DefaultPredictionRepository extends JpaRepository<DefaultPrediction, Long> {
}
