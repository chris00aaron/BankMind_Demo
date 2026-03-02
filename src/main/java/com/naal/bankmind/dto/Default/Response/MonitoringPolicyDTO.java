package com.naal.bankmind.dto.Default.Response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MonitoringPolicyDTO {

    private Long idMonitoringPolicy;
    private String policyName;
    private BigDecimal psiThreshold;
    private Integer consecutiveDaysTrigger;
    private BigDecimal aucDropThreshold;
    private BigDecimal ksDropThreshold;
    private Integer optunaTrialsDrift;
    private Integer optunaTrialsValidation;
    private LocalDate activationDate;
    private LocalDate cancellationDate;
    private Boolean isActive;
    private String createdBy;
}
