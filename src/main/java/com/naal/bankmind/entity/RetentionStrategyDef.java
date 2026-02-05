package com.naal.bankmind.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "retention_strategy_def")
public class RetentionStrategyDef {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_strategy")
    @JsonProperty("id")
    private Long idStrategy;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "cost_per_client")
    private BigDecimal costPerClient;

    @Column(name = "impact_factor")
    private BigDecimal impactFactor;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
