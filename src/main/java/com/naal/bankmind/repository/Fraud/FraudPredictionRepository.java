package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.FraudPredictions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad FraudPredictions
 */
@Repository
public interface FraudPredictionRepository extends JpaRepository<FraudPredictions, Long> {

        /**
         * Buscar predicción por ID de transacción
         */
        Optional<FraudPredictions> findByTransactionIdTransaction(Long transactionId);

        /**
         * Buscar predicciones por veredicto (ALTO RIESGO / LEGÍTIMO)
         */
        List<FraudPredictions> findByVeredicto(String veredicto);

        /**
         * Verificar si ya existe una predicción para una transacción
         */
        boolean existsByTransactionIdTransaction(Long transactionId);

        // ==================== MÉTODOS PAGINADOS ====================

        /**
         * Obtener todas las predicciones paginadas con detalles de transacción
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "ORDER BY fp.predictionDate DESC")
        List<FraudPredictions> findAllWithDetails();

        /**
         * Obtener predicciones paginadas ordenadas por score (más riesgosas primero)
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c")
        Page<FraudPredictions> findAllWithDetailsPaged(Pageable pageable);

        /**
         * Filtrar por veredicto con paginación
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "WHERE fp.veredicto = :veredicto")
        Page<FraudPredictions> findByVeredictoWithDetailsPaged(
                        @Param("veredicto") String veredicto,
                        Pageable pageable);

        /**
         * Buscar por ID de transacción (trans_num) con paginación
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "WHERE LOWER(t.transNum) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<FraudPredictions> searchByTransactionId(
                        @Param("search") String search,
                        Pageable pageable);

        /**
         * Buscar con filtro de veredicto y búsqueda combinados
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "WHERE fp.veredicto = :veredicto " +
                        "AND LOWER(t.transNum) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<FraudPredictions> searchByVeredictoAndTransactionId(
                        @Param("veredicto") String veredicto,
                        @Param("search") String search,
                        Pageable pageable);

        /**
         * Contar por veredicto
         */
        long countByVeredicto(String veredicto);

        /**
         * Obtener predicción con todos los detalles SHAP
         */
        @Query("SELECT fp FROM FraudPredictions fp " +
                        "LEFT JOIN FETCH fp.details " +
                        "LEFT JOIN FETCH fp.transaction t " +
                        "LEFT JOIN FETCH t.creditCard cc " +
                        "LEFT JOIN FETCH cc.customer c " +
                        "WHERE fp.idFraudPrediction = :id")
        Optional<FraudPredictions> findByIdWithDetails(@Param("id") Long id);

        // ==================== MÉTODOS PARA DASHBOARD STATS ====================

        /**
         * Contar todas las predicciones
         */
        long count();

        /**
         * Obtener suma de montos de transacciones fraudulentas
         */
        @Query("SELECT COALESCE(SUM(t.amt), 0) FROM FraudPredictions fp " +
                        "JOIN fp.transaction t " +
                        "WHERE fp.veredicto = 'ALTO RIESGO'")
        Double sumAmountAtRisk();

        /**
         * Obtener promedio de score de fraudes detectados
         */
        @Query("SELECT COALESCE(AVG(fp.xgboostScore), 0) FROM FraudPredictions fp " +
                        "WHERE fp.veredicto = 'ALTO RIESGO'")
        Double avgFraudScore();

        /**
         * Obtener estadísticas por hora (nativas para mejor rendimiento)
         */
        @Query(value = "SELECT " +
                        "EXTRACT(HOUR FROM t.trans_date_time) as hora, " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN fp.veredicto = 'ALTO RIESGO' THEN 1 ELSE 0 END) as fraudes " +
                        "FROM operational_transactions t " +
                        "LEFT JOIN fraud_predictions fp ON t.id_transaction = fp.id_transaction " +
                        "GROUP BY hora " +
                        "ORDER BY hora", nativeQuery = true)
        List<Object[]> getHourlyTrendStats();

        /**
         * Obtener estadísticas por categoría
         */
        @Query(value = "SELECT " +
                        "c.category_name as category, " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN fp.veredicto = 'ALTO RIESGO' THEN 1 ELSE 0 END) as fraudes, " +
                        "COALESCE(SUM(t.amt), 0) as monto_total " +
                        "FROM operational_transactions t " +
                        "LEFT JOIN categories c ON t.id_category = c.id_category " +
                        "LEFT JOIN fraud_predictions fp ON t.id_transaction = fp.id_transaction " +
                        "WHERE c.category_name IS NOT NULL " +
                        "GROUP BY c.category_name " +
                        "ORDER BY fraudes DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> getCategoryStats();

        /**
         * Obtener estadísticas por ubicación (estado)
         */
        @Query(value = "SELECT " +
                        "l.state, " +
                        "COUNT(*) as total, " +
                        "SUM(CASE WHEN fp.veredicto = 'ALTO RIESGO' THEN 1 ELSE 0 END) as fraudes " +
                        "FROM fraud_predictions fp " +
                        "JOIN operational_transactions t ON fp.id_transaction = t.id_transaction " +
                        "JOIN credit_cards cc ON t.cc_num = cc.cc_num " +
                        "JOIN customers c ON cc.id_customer = c.id_customer " +
                        "JOIN localizations l ON c.id_localization = l.id_localization " +
                        "GROUP BY l.state " +
                        "ORDER BY fraudes DESC " +
                        "LIMIT 10", nativeQuery = true)
        List<Object[]> getLocationStats();
}
