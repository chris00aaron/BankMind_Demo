package com.naal.bankmind.repository.Default;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Default.DefaultPrediction;

@Repository
public interface DefaultPredictionRepository extends JpaRepository<DefaultPrediction, Long> {

        /**
         * Cuenta predicciones con probabilidad de default menor al valor dado.
         * Usado para calcular el percentil de riesgo.
         */
        long countByDefaultProbabilityLessThan(BigDecimal probability);

        @Query("SELECT p FROM DefaultPrediction p JOIN FETCH p.monthlyHistory JOIN FETCH p.idPolicy " +
                        "WHERE p.datePrediction BETWEEN :startDate AND :endDate AND p.idProductionModel.idProductionModel = :modelId")
        java.util.List<DefaultPrediction> findPredictionsForValidation(java.time.LocalDateTime startDate,
                        java.time.LocalDateTime endDate, Long modelId);

        /**
         * Obtiene todas las predicciones de una cuenta ordenadas cronológicamente
         * (ASC).
         * Usado para el gráfico de timeline de predicción individual.
         */
        @Query("SELECT p FROM DefaultPrediction p " +
                        "JOIN FETCH p.monthlyHistory mh " +
                        "WHERE mh.accountDetails.recordId = :recordId " +
                        "ORDER BY p.datePrediction ASC")
        java.util.List<DefaultPrediction> findByRecordIdOrderByDateAsc(
                        @Param("recordId") Long recordId);

        /**
         * Obtiene todas las predicciones de una cuenta ordenadas por fecha descendente.
         * Incluye datos del cliente para ser usado directamente en mapeo a DTO.
         */
        @Query("SELECT p FROM DefaultPrediction p " +
                        "JOIN FETCH p.monthlyHistory mh " +
                        "JOIN FETCH mh.accountDetails ad " +
                        "JOIN FETCH ad.customer " +
                        "WHERE mh.accountDetails.recordId = :recordId " +
                        "ORDER BY p.datePrediction DESC")
        java.util.List<DefaultPrediction> findByRecordIdOrderByDateDesc(
                        @Param("recordId") Long recordId);
}
