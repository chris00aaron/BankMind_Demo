package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.CreditCards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para CreditCards
 */
@Repository
public interface CreditCardRepository extends JpaRepository<CreditCards, Long> {

    /**
     * Buscar tarjeta con datos del cliente y ubicación precargados
     */
    @Query("SELECT c FROM CreditCards c " +
            "LEFT JOIN FETCH c.customer cust " +
            "LEFT JOIN FETCH cust.localization l " +
            "LEFT JOIN FETCH cust.gender g " +
            "WHERE c.ccNum = :ccNum")
    Optional<CreditCards> findByIdWithCustomerData(Long ccNum);

    /**
     * Contar tarjetas bloqueadas
     * Para dashboard: mostrar tarjetas bloqueadas por fraude
     */
    long countByIsActive(Boolean isActive);
}
