package com.naal.bankmind.dto.Default.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para políticas de default.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DefaultPoliciesDTO {

    private Long idPolicy;
    private String policyName;
    private BigDecimal thresholdApproval;
    private BigDecimal factorLgd;
    private Integer daysGraceDefault;
    private LocalDate activationDate;
    private LocalDate cancellationDate;
    private Boolean isActive;
    private String approvedBy;
    private List<ClassificationRuleSBSDTO> sbsClassificationMatrix;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationRuleSBSDTO {
        private String categoria;
        private Double min;
        private Double max;
        private Double provision;
    }
}
