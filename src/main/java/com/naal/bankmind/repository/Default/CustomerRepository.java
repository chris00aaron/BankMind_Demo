package com.naal.bankmind.repository.Default;

import com.naal.bankmind.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
}
