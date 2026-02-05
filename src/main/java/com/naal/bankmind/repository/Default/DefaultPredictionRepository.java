package com.naal.bankmind.repository.Default;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Default.DefaultPrediction;

@Repository
public interface DefaultPredictionRepository extends JpaRepository<DefaultPrediction, Long> {

    /**
     * Cuenta predicciones con probabilidad de default menor al valor dado.
     * Usado para calcular el percentil de riesgo.
     */
    long countByDefaultProbabilityLessThan(BigDecimal probability);
}
