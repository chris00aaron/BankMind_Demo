package com.naal.bankmind.dto.Churn;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GeographyStatsDTO {
    private String country;
    private String countryCode;
    private String flag;
    private int totalCustomers;
    private int highRisk; // > 70%
    private int mediumRisk; // 45-70%
    private int lowRisk; // < 45%
    private BigDecimal avgBalance;
    private BigDecimal churnRate;
}
