package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the response from the Python Auto-Training API.
 * Maps the JSON response from POST /churn/train.
 *
 * NOTE: @JsonAlias is used (not @JsonProperty) on fields that are manually
 * mapped in ChurnService.trainModel(). This ensures Jackson serializes back
 * to the frontend using camelCase field names (what TypeScript expects),
 * while still accepting snake_case aliases during deserialization if needed.
 *
 * Champion/Challenger fields:
 *   promoted         — true if the challenger replaced the champion in production
 *   promotionReason  — human-readable explanation of the decision
 *   championMetrics  — metrics of the model that was in production before this run
 *   inProduction     — same as promoted; stored in churn_training_history
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResultDTO {
    private String  status;       // "success" | "error"
    private String  message;
    private String  runId;
    private TrainMetrics metrics;
    private Integer trainSamples;
    private Integer testSamples;
    private String  error;
    private String  versionTag;

    // Champion / Challenger
    private Boolean promoted;
    private String  promotionReason;
    private Boolean inProduction;
    private ChampionMetrics championMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainMetrics {
        private Double accuracy;
        private Double f1Score;
        private Double precision;
        private Double recall;
        private Double aucRoc;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChampionMetrics {
        // @JsonAlias allows Jackson to read snake_case from Python if ever used
        // directly, but serializes as camelCase to the frontend.
        @JsonAlias("auc_roc")
        private Double aucRoc;
        @JsonAlias("f1_score")
        private Double f1Score;
        @JsonAlias("accuracy")
        private Double accuracy;
        @JsonAlias("precision_score")
        private Double precisionScore;
        @JsonAlias("recall_score")
        private Double recallScore;
        @JsonAlias("model_version")
        private String modelVersion;
    }
}
