package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the response from the Python Auto-Training API.
 * Maps the JSON response from POST /churn/train.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainResultDTO {
    private String status; // "success" | "error"
    private String message;
    private String runId; // MLflow run ID
    private TrainMetrics metrics;
    private Integer trainSamples;
    private Integer testSamples;
    private String error; // Error message if failed

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
}
