package com.naal.bankmind.dto.Churn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for campaign log data exchanged with the frontend.
 * Maps to/from the CampaignLog entity + denormalized names.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLogDTO {

    private Long id;

    private String name;

    @JsonProperty("segmentName")
    private String segmentName;

    @JsonProperty("strategyName")
    private String strategyName;

    @JsonProperty("startDate")
    private String startDate;

    private String status;

    @JsonProperty("budgetAllocated")
    private Double budgetAllocated;

    @JsonProperty("expectedRoi")
    private Double expectedRoi;

    @JsonProperty("targetedCount")
    private Integer targetedCount;

    @JsonProperty("convertedCount")
    private Integer convertedCount;

    // Request fields (for creation only)
    @JsonProperty("segmentId")
    private Integer segmentId;

    @JsonProperty("strategyId")
    private Long strategyId;

    private Double budget;

    private java.util.List<Long> targets;
}
