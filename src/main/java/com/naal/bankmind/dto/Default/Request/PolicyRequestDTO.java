package com.naal.bankmind.dto.Default.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para crear o actualizar una política.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequestDTO {

    private String policyName;
    private BigDecimal thresholdApproval;
    private BigDecimal factorLgd;
    private Integer daysGraceDefault;
    private String approvedBy;
    private List<ClassificationRuleDTO> sbsClassificationMatrix;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationRuleDTO {
        private String categoria;
        private Double min;
        private Double max;
        private Double provision;
    }
}
