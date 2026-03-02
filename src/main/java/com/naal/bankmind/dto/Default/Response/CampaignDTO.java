package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDTO {
    private Long idCampaign;
    private String campaignName;
    private String description;
    private String targetSegment;
    private BigDecimal reductionFactor;
    private BigDecimal estimatedCost;
    private Boolean isActive;
    private LocalDateTime createdDate;
}
