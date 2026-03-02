package com.naal.bankmind.entity.Default;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mitigation_campaign")
public class MitigationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_campaign")
    private Long idCampaign;

    @Column(name = "campaign_name", length = 100, nullable = false)
    private String campaignName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_segment", length = 20, nullable = false)
    private String targetSegment;

    @Column(name = "reduction_factor", precision = 5, scale = 4, nullable = false)
    private BigDecimal reductionFactor;

    @Column(name = "estimated_cost", precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null)
            createdDate = LocalDateTime.now();
        if (isActive == null)
            isActive = true;
    }
}
