package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for the Performance Monitor status response.
 * Maps the JSON response from GET /churn/monitor/status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceStatusDTO {
    private String status; // "healthy" | "degraded" | "insufficient_data" | "no_evaluations" | "error"
    private String message;
    private Double recall;
    private Double f1Score;
    private Double precision;
    private Double accuracy;
    private Double recallThreshold;
    private Integer evaluatedSamples;
    private Integer minSamplesRequired;
    private Integer maturationDays;
    private Boolean monitorEnabled;
    private Integer monitorIntervalHours;
    private String lastEvaluationDate;
    private String nextEvaluationDate;
    private Boolean autoTrainingTriggered;
    private String triggerReason;

    // Confusion matrix
    private Integer truePositives;
    private Integer falsePositives;
    private Integer trueNegatives;
    private Integer falseNegatives;

    // Training result (if auto-retrain was triggered)
    private String trainingRunId;
    private String trainingError;
}
