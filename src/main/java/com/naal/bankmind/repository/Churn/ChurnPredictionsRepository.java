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
     * Gets the latest prediction for each customer in a list of IDs.
     * Uses MAX(id) per customer group to safely pick one row per customer,
     * avoiding duplicate-date issues and cross-join problems in PostgreSQL.
     */
    @Query("SELECT cp FROM ChurnPredictions cp " +
            "WHERE cp.idChurnPrediction IN (" +
            "  SELECT MAX(cp2.idChurnPrediction) FROM ChurnPredictions cp2 " +
            "  WHERE cp2.customer.idCustomer IN :customerIds " +
            "  GROUP BY cp2.customer.idCustomer" +
            ")")
    List<ChurnPredictions> findLatestByCustomerIds(@Param("customerIds") List<Long> customerIds);
}
