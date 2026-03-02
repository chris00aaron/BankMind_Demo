package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "fraud_confirmation_tokens")
public class FraudConfirmationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_token")
    private Long idToken;

    @Column(name = "token", unique = true, nullable = false, length = 100)
    private String token;

    @ManyToOne
    @JoinColumn(name = "id_transaction")
    private OperationalTransactions transaction;

    @ManyToOne
    @JoinColumn(name = "id_prediction")
    private FraudPredictions prediction;

    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType; // 'CONFIRM' o 'BLOCK'

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "is_used")
    private Boolean isUsed = false;

    // Helper methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isUsed && !isExpired();
    }
}
