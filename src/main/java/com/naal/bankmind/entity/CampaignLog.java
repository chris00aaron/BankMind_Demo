package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "campaign_log")
public class CampaignLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_campaign")
    private Long idCampaign;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "budget_allocated")
    private BigDecimal budgetAllocated;

    // FK to retention_strategy_def (already exists in DB)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_strategy", referencedColumnName = "id_strategy")
    private RetentionStrategyDef strategy;

    // FK to retention_segment_def (already exists in DB)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_segment", referencedColumnName = "id_segment")
    private RetentionSegmentDef segment;
}
