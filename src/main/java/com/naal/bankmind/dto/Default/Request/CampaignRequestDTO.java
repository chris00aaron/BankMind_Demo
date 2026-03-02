package com.naal.bankmind.dto.Default.Request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CampaignRequestDTO {
    private String campaignName;
    private String description;
    private String targetSegment;
    private BigDecimal reductionFactor;
    private BigDecimal estimatedCost;
}
