package com.naal.bankmind.dto.Churn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a single probability bucket in the churn prediction distribution chart.
 * Example: bucket "20-30%", count 145
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionBucketDTO {
    private String bucket; // e.g. "20-30%"
    private int count;
}
