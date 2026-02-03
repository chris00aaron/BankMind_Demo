package com.naal.bankmind.repository.Fraud;

import com.naal.bankmind.entity.FraudConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FraudConfirmationTokenRepository extends JpaRepository<FraudConfirmationToken, Long> {

    /**
     * Buscar token por su valor único
     */
    Optional<FraudConfirmationToken> findByToken(String token);

    /**
     * Buscar token por ID de transacción
     */
    Optional<FraudConfirmationToken> findByTransactionIdTransaction(Long transactionId);

    /**
     * Eliminar tokens expirados (útil para limpieza periódica)
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    /**
     * Contar tokens usados por tipo de acción
     * Para dashboard: medir feedback de clientes
     */
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(t) FROM FraudConfirmationToken t " +
            "WHERE t.isUsed = true AND t.tokenType = :tokenType")
    long countUsedByTokenType(String tokenType);
}
