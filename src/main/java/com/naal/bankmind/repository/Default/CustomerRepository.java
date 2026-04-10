package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

        /**
         * Busca clientes por nombre (firstName o surname) o por ID.
         * La búsqueda es case-insensitive y parcial.
         */
        @Query("SELECT c FROM Customer c WHERE " +
                        "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "LOWER(c.surname) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
                        "CAST(c.idCustomer AS string) LIKE CONCAT('%', :searchTerm, '%')")
        List<Customer> searchByNameOrId(@Param("searchTerm") String searchTerm);

        /**
         * Versión paginada de searchByNameOrId para el dashboard.
         */
        @Query("SELECT c FROM Customer c WHERE " +
                        "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "CAST(c.idCustomer AS string) LIKE CONCAT('%', :search, '%')")
        Page<Customer> searchByNameOrIdPageable(@Param("search") String search, Pageable pageable);

        /**
         * Busca clientes con filtros dinámicos para predicción por lotes.
         * Usa native query para evitar problemas con parámetros NULL en PostgreSQL.
         */
        @Query(value = "SELECT * FROM customer c WHERE " +
                        "(CAST(:edadMin AS INTEGER) IS NULL OR c.age >= :edadMin) AND " +
                        "(CAST(:edadMax AS INTEGER) IS NULL OR c.age <= :edadMax) AND " +
                        "(CAST(:educacion AS INTEGER) IS NULL OR c.id_education = :educacion) AND " +
                        "(CAST(:estadoCivil AS INTEGER) IS NULL OR c.id_marriage = :estadoCivil) AND " +
                        "(CAST(:fechaDesde AS TIMESTAMP) IS NULL OR c.id_registration_date >= :fechaDesde) AND " +
                        "(CAST(:fechaHasta AS TIMESTAMP) IS NULL OR c.id_registration_date <= :fechaHasta)", nativeQuery = true)
        List<Customer> findByFilters(
                        @Param("edadMin") Integer edadMin,
                        @Param("edadMax") Integer edadMax,
                        @Param("educacion") Integer educacion,
                        @Param("estadoCivil") Integer estadoCivil,
                        @Param("fechaDesde") LocalDateTime fechaDesde,
                        @Param("fechaHasta") LocalDateTime fechaHasta);

        /**
         * Returns customer IDs filtered by country description.
         * Used for pre-filtering before pagination in Centro de Mando.
         */
        @Query("SELECT c.idCustomer FROM Customer c WHERE LOWER(c.country.countryDescription) = LOWER(:country)")
        List<Long> findIdsByCountry(@Param("country") String country);

        /**
         * Paginated query over a specific set of customer IDs.
         * Used after pre-filtering by riskLevel / country.
         */
        Page<Customer> findByIdCustomerIn(Collection<Long> ids, Pageable pageable);

        /**
         * Paginated search restricted to a specific set of customer IDs.
         * Combines text search with pre-filtered ID set.
         */
        @Query("SELECT c FROM Customer c WHERE c.idCustomer IN :ids AND (" +
                        "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(c.surname)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "CAST(c.idCustomer AS string) LIKE CONCAT('%', :search, '%'))")
        Page<Customer> findByIdCustomerInAndSearch(
                        @Param("ids") Collection<Long> ids,
                        @Param("search") String search,
                        Pageable pageable);

        // ── Risk-sorted queries (show highest-risk customers first) ────────────

        /**
         * All customers sorted by latest churn_probability DESC.
         * Customers without prediction appear last (probability treated as -1).
         */
        @Query(value = "SELECT c.* FROM customer c " +
                        "LEFT JOIN (" +
                        "  SELECT DISTINCT ON (id_customer) id_customer, churn_probability " +
                        "  FROM churn_predictions " +
                        "  ORDER BY id_customer, id_churn_prediction DESC" +
                        ") lp ON lp.id_customer = c.id_customer " +
                        "ORDER BY COALESCE(lp.churn_probability, -1) DESC",
                countQuery = "SELECT COUNT(*) FROM customer",
                nativeQuery = true)
        Page<Customer> findAllOrderByRiskDesc(Pageable pageable);

        /**
         * Pre-filtered customers (by eligibleIds) sorted by latest churn_probability DESC.
         */
        @Query(value = "SELECT c.* FROM customer c " +
                        "LEFT JOIN (" +
                        "  SELECT DISTINCT ON (id_customer) id_customer, churn_probability " +
                        "  FROM churn_predictions " +
                        "  ORDER BY id_customer, id_churn_prediction DESC" +
                        ") lp ON lp.id_customer = c.id_customer " +
                        "WHERE c.id_customer IN :ids " +
                        "ORDER BY COALESCE(lp.churn_probability, -1) DESC",
                countQuery = "SELECT COUNT(*) FROM customer WHERE id_customer IN :ids",
                nativeQuery = true)
        Page<Customer> findByIdCustomerInOrderByRiskDesc(
                        @Param("ids") Collection<Long> ids,
                        Pageable pageable);

        /**
         * Pre-filtered + text search, sorted by latest churn_probability DESC.
         */
        @Query(value = "SELECT c.* FROM customer c " +
                        "LEFT JOIN (" +
                        "  SELECT DISTINCT ON (id_customer) id_customer, churn_probability " +
                        "  FROM churn_predictions " +
                        "  ORDER BY id_customer, id_churn_prediction DESC" +
                        ") lp ON lp.id_customer = c.id_customer " +
                        "WHERE c.id_customer IN :ids " +
                        "AND (LOWER(c.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR CAST(c.id_customer AS VARCHAR) LIKE CONCAT('%', :search, '%')) " +
                        "ORDER BY COALESCE(lp.churn_probability, -1) DESC",
                countQuery = "SELECT COUNT(*) FROM customer c " +
                        "WHERE c.id_customer IN :ids " +
                        "AND (LOWER(c.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR CAST(c.id_customer AS VARCHAR) LIKE CONCAT('%', :search, '%'))",
                nativeQuery = true)
        Page<Customer> findByIdCustomerInAndSearchOrderByRiskDesc(
                        @Param("ids") Collection<Long> ids,
                        @Param("search") String search,
                        Pageable pageable);

        /**
         * Text search (no ID filter) sorted by latest churn_probability DESC.
         */
        @Query(value = "SELECT c.* FROM customer c " +
                        "LEFT JOIN (" +
                        "  SELECT DISTINCT ON (id_customer) id_customer, churn_probability " +
                        "  FROM churn_predictions " +
                        "  ORDER BY id_customer, id_churn_prediction DESC" +
                        ") lp ON lp.id_customer = c.id_customer " +
                        "WHERE (LOWER(c.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR CAST(c.id_customer AS VARCHAR) LIKE CONCAT('%', :search, '%')) " +
                        "ORDER BY COALESCE(lp.churn_probability, -1) DESC",
                countQuery = "SELECT COUNT(*) FROM customer c " +
                        "WHERE (LOWER(c.first_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR LOWER(c.surname) LIKE LOWER(CONCAT('%', :search, '%')) " +
                        "  OR CAST(c.id_customer AS VARCHAR) LIKE CONCAT('%', :search, '%'))",
                nativeQuery = true)
        Page<Customer> searchByNameOrIdOrderByRiskDesc(
                        @Param("search") String search,
                        Pageable pageable);
}
