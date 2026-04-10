package com.naal.bankmind.repository.Churn;

import com.naal.bankmind.entity.ChurnPredictions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChurnPredictionsRepository extends JpaRepository<ChurnPredictions, Long> {

    /**
     * Gets all churn predictions for a customer, ordered by date descending.
     */
    List<ChurnPredictions> findByCustomer_IdCustomerOrderByPredictionDateDesc(Long idCustomer);

    /**
     * Counts total predictions for a customer.
     */
    long countByCustomer_IdCustomer(Long idCustomer);

    /**
     * Gets predictions with customer data in a single query.
     */
    @Query("SELECT cp FROM ChurnPredictions cp JOIN FETCH cp.customer c WHERE cp.customer.idCustomer = :idCustomer ORDER BY cp.predictionDate DESC")
    List<ChurnPredictions> findAllWithCustomerByIdCustomer(@Param("idCustomer") Long idCustomer);

    /**
     * Returns the latest churn probability per customer for the distribution histogram.
     * DISTINCT ON uses idx_churn_pred_customer_id_desc — evita HashAggregate sobre 39k filas.
     */
    @Query(value = "SELECT DISTINCT ON (id_customer) churn_probability " +
            "FROM churn_predictions " +
            "WHERE churn_probability IS NOT NULL " +
            "ORDER BY id_customer, id_churn_prediction DESC",
            nativeQuery = true)
    List<java.math.BigDecimal> findLatestChurnProbabilitiesPerCustomer();

    /**
     * Gets the latest prediction for each customer in a list of IDs.
     * DISTINCT ON + idx_churn_pred_customer_id_desc — Index Scan en lugar de HashAggregate.
     */
    @Query(value = "SELECT DISTINCT ON (cp.id_customer) cp.* " +
            "FROM churn_predictions cp " +
            "WHERE cp.id_customer IN :customerIds " +
            "ORDER BY cp.id_customer, cp.id_churn_prediction DESC",
            nativeQuery = true)
    List<ChurnPredictions> findLatestByCustomerIds(@Param("customerIds") List<Long> customerIds);

    /**
     * Returns the latest prediction for ALL customers that have at least one prediction.
     * DISTINCT ON usa el índice compuesto — evita subquery MAX + HashAggregate.
     */
    @Query(value = "SELECT DISTINCT ON (cp.id_customer) cp.* " +
            "FROM churn_predictions cp " +
            "ORDER BY cp.id_customer, cp.id_churn_prediction DESC",
            nativeQuery = true)
    List<ChurnPredictions> findLatestForAllCustomers();

    /**
     * Returns customer IDs whose latest prediction matches the given risk level.
     * Subquery sobre DISTINCT ON — evita doble GroupBy.
     */
    @Query(value = "SELECT id_customer FROM (" +
            "  SELECT DISTINCT ON (id_customer) id_customer, risk_level " +
            "  FROM churn_predictions " +
            "  ORDER BY id_customer, id_churn_prediction DESC" +
            ") latest WHERE LOWER(risk_level) = LOWER(:riskLevel)",
            nativeQuery = true)
    List<Long> findCustomerIdsByLatestRiskLevel(@Param("riskLevel") String riskLevel);
}
