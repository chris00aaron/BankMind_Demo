package com.naal.bankmind.entity.atm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "atm_features", schema = "public", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "id_transaction", "reference_date" })
})
public class AtmFeatures {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_feature_store")
    private Long idFeatureStore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transaction", 
                referencedColumnName = "id_transaction",
                foreignKey = @ForeignKey(name = "fk_atm_features-daily_atm_transaction"),
                nullable = false)
    private DailyAtmTransaction transaction;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "withdrawal_amount_day", precision = 15, scale = 3)
    private BigDecimal withdrawalAmountDay;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dynamic_features", columnDefinition = "jsonb")
    private Map<String, Object> dynamicFeatures;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}