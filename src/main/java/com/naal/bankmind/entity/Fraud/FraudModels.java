package com.naal.bankmind.entity.Fraud;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@Table(name = "fraud_models")
public class FraudModels {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_model")
    private Long idModel;

    @Column(name = "model_version", nullable = false, unique = true, length = 50)
    private String modelVersion;

    @Column(name = "dagshub_url", length = 500)
    private String dagshubUrl;

    @Column(name = "file_path", length = 255)
    private String filePath;

    @Column(name = "model_size_mb")
    private BigDecimal modelSizeMb;

    @Column(name = "algorithm", nullable = false, length = 50)
    private String algorithm;

    // JSONB in Postgres, mapped as String here for simplicity.
    // Could use a custom converter or library for JSON mapping if needed.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_config", columnDefinition = "jsonb")
    private String modelConfig;

    @Column(name = "threshold")
    private BigDecimal threshold;

    @Column(name = "promotion_status", nullable = false, length = 20)
    private String promotionStatus; // 'CHALLENGER' default handled by DB or Service

    @Column(name = "is_active")
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_model_id")
    private FraudModels predecessorModel;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "promoted_at")
    private LocalDateTime promotedAt;
}
