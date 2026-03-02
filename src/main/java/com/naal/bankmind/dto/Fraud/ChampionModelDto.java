package com.naal.bankmind.dto.Fraud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO del modelo CHAMPION activo.
 * Alimenta la sección "Modelo en Producción" en el Frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChampionModelDto {

    @JsonProperty("id_model")
    private Long idModel;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("algorithm")
    private String algorithm;

    @JsonProperty("threshold")
    private BigDecimal threshold;

    @JsonProperty("promotion_status")
    private String promotionStatus;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("promoted_at")
    private LocalDateTime promotedAt;

    /** Métricas del último ciclo de entrenamiento de este champion */
    @JsonProperty("accuracy")
    private BigDecimal accuracy;

    @JsonProperty("precision_score")
    private BigDecimal precisionScore;

    @JsonProperty("recall_score")
    private BigDecimal recallScore;

    @JsonProperty("f1_score")
    private BigDecimal f1Score;

    @JsonProperty("auc_roc")
    private BigDecimal aucRoc;
}
