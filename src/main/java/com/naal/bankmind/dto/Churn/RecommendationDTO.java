package com.naal.bankmind.dto.Churn;

import java.math.BigDecimal;

/**
 * DTO for the Next Best Action recommendation.
 * Mirrors RetentionStrategyDef but with a dynamically computed impactFactor
 * (calculated by simulating the intervention with the ML model).
 * Using a DTO avoids Hibernate dirty-checking from persisting runtime values.
 */
public class RecommendationDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal costPerClient;
    private BigDecimal impactFactor;
    private Boolean isActive;

    public RecommendationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getCostPerClient() { return costPerClient; }
    public void setCostPerClient(BigDecimal costPerClient) { this.costPerClient = costPerClient; }

    public BigDecimal getImpactFactor() { return impactFactor; }
    public void setImpactFactor(BigDecimal impactFactor) { this.impactFactor = impactFactor; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
