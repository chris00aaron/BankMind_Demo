package com.naal.bankmind.repository.atm;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.naal.bankmind.entity.atm.DailyWithdrawalPrediction;

public interface DailyWithdrawalPredictionRepository extends JpaRepository<DailyWithdrawalPrediction, Long> {

    List<DailyWithdrawalPrediction> findByPredictionDate(LocalDate predictionDate);
}
