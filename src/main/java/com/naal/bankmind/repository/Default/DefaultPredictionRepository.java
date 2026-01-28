package com.naal.bankmind.repository.Default;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Default.DefaultPrediction;

@Repository
public interface DefaultPredictionRepository extends JpaRepository<DefaultPrediction, Long> {
}
