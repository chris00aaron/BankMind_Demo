package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single data point in the training history evolution chart.
 * Metrics are returned as 0-100 scale for direct frontend display.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingHistoryPointDTO {
    private String date;           // "2024-01-15"
    private String triggerReason;  // "manual_training" | "performance_decay" | "scheduled_check"
    private Double recall;
    private Double precision;
    private Double f1Score;
    private Double accuracy;
    private Double aucRoc;
    private Boolean inProduction;
    private Integer evaluatedSamples;
}
