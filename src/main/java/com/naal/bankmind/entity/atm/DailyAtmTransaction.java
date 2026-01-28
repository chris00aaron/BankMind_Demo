package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "daily_atm_transactions", schema = "public")
public class DailyAtmTransaction {
    @Id
    @Column(name = "id_transaction")
    private Long idTransaction; // ID viene del banco central

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_atm", 
            referencedColumnName = "id_atm", // Columna en la tabla atms (PK)
            foreignKey = @ForeignKey(name = "fk_atm_transaction-atm"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private Atm atm;

    @Column(name = "amount", nullable = false, precision = 15, scale = 3)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType type;

    @Column(name = "balance_after", precision = 15, scale = 3)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_weather",
            referencedColumnName = "id_weather", // Columna en la tabla weather (PK)
            foreignKey = @ForeignKey(name = "fk_atm_transaction-weather"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private Weather weather;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sync",
            referencedColumnName = "id_sync", // Columna en la tabla sync_logs (PK)
            foreignKey = @ForeignKey(name = "fk_atm_transaction-sync_log"), // Nombre de la restricción de clave foránea
            nullable = false // No puede ser null
    )
    private SyncLog syncLog;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}