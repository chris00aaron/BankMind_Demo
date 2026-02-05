package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "campaign_target")
public class CampaignTarget {

    @EmbeddedId
    private CampaignTargetKey id;

    @Column(name = "status")
    private String status;

    @Column(name = "contact_date")
    private LocalDateTime contactDate;

    @Column(name = "response_date")
    private LocalDateTime responseDate;
}
