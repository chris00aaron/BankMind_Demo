package com.naal.bankmind.dto.Default.Request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonitoringPolicyRequestDTO {

    private String policyName;
    private BigDecimal psiThreshold;
    private Integer consecutiveDaysTrigger;
    private BigDecimal aucDropThreshold;
    private BigDecimal ksDropThreshold;
    private Integer optunaTrialsDrift;
    private Integer optunaTrialsValidation;
    private String createdBy;
}
