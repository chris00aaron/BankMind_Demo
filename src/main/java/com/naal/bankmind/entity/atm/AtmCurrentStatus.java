package com.naal.bankmind.entity.atm;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = "atm")
@Entity
@Table(name = "atm_current_status", schema = "public")
public class AtmCurrentStatus {

        @Id
        @Column(name = "id_atm")
        private Long idAtm;

        @JsonBackReference("atm-currentStatus")
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @MapsId // Indica que la clave primaria de esta entidad es la misma que la de la entidad
                // relacionada
        @JoinColumn(name = "id_atm", // Columna en la tabla atm_current_status (FK)
                        referencedColumnName = "id_atm", // Columna en la tabla atms (PK)
                        foreignKey = @ForeignKey(name = "fk_atm_current_status-atm"), // Nombre de la restricción de
                                                                                      // clave foránea
                        nullable = false // No puede ser null
        )
        private Atm atm;

        @Column(name = "current_balance", nullable = false, precision = 15, scale = 3)
        private BigDecimal currentBalance = BigDecimal.ZERO;

        @Column(name = "last_transaction_date")
        private LocalDateTime lastTransactionDate;

        @Column(name = "last_withdrawal_date")
        private LocalDateTime lastWithdrawalDate;

        @Column(name = "last_deposit_date")
        private LocalDateTime lastDepositDate;

        @Column(name = "last_reload_date")
        private LocalDateTime lastReloadDate;

        @JsonBackReference("syncLog-atmStatuses")
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "last_sync_id", // Columna en la tabla atm_current_status (FK)
                        referencedColumnName = "id_sync", // Columna en la tabla sync_logs (PK)
                        foreignKey = @ForeignKey(name = "fk_atm_current_status-sync_log") // Nombre de la restricción de
                                                                                          // clave
                                                                                          // foránea
        )
        private SyncLog lastSync;

        @Column(name = "updated_at")
        private LocalDateTime updatedAt = LocalDateTime.now();

        // Método de utilidad para calcular porcentaje de capacidad
        public BigDecimal getCapacityPercentage() {
                if (atm == null || atm.getMaxCapacity() == null ||
                                atm.getMaxCapacity().compareTo(BigDecimal.ZERO) == 0) {
                        return null;
                }
                return currentBalance
                                .multiply(BigDecimal.valueOf(100))
                                .divide(atm.getMaxCapacity(), 2, java.math.RoundingMode.HALF_UP);
        }
}
