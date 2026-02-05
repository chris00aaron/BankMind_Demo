package com.naal.bankmind.dto.Churn;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MLOpsMetricsDTO {
    private String modelStatus;
    private String modelVersion;
    private long totalPredictions;
    private String lastTrainingDate;

    // Performance Metrics
    private double precision;
    private double recall;
    private double f1Score;
    private double aucRoc;
}
