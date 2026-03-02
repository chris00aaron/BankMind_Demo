package com.naal.bankmind.repository.Fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.naal.bankmind.entity.Fraud.PredictionDetails;

import java.util.List;

/**
 * Repositorio JPA para la entidad PredictionDetails (factores SHAP)
 */
@Repository
public interface PredictionDetailsRepository extends JpaRepository<PredictionDetails, Long> {

    /**
     * Buscar detalles por ID de predicción de fraude
     */
    List<PredictionDetails> findByFraudPredictionIdFraudPrediction(Long fraudPredictionId);

    /**
     * Eliminar detalles por ID de predicción de fraude
     */
    void deleteByFraudPredictionIdFraudPrediction(Long fraudPredictionId);

    /**
     * Obtener promedio global de valores SHAP por feature (para dashboard)
     * Solo considera valores positivos (que aumentan riesgo)
     */
    @Query(value = "SELECT " +
            "pd.feature_name, " +
            "AVG(ABS(pd.shap_value)) as avg_impact, " +
            "COUNT(*) as occurrences " +
            "FROM prediction_details pd " +
            "WHERE pd.shap_value > 0 " +
            "GROUP BY pd.feature_name " +
            "ORDER BY avg_impact DESC " +
            "LIMIT 5", nativeQuery = true)
    List<Object[]> getGlobalShapStats();
}
