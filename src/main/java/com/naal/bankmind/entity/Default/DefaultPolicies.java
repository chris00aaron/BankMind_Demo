package com.naal.bankmind.entity.Default;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.naal.bankmind.entity.Default.POJO.ClassficationRuleSBS;

@Data
@Entity
@Table(name = "default_policies")
public class DefaultPolicies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_policy")
    private Long idPolicy;

    @Column(name = "policy_name", length = 50)
    private String policyName;

    @Column(name = "threshold_approval", precision = 5, scale = 4)
    private BigDecimal thresholdApproval;

    @Column(name = "factor_lgd", precision = 5, scale = 4)
    private BigDecimal factorLgd;

    @Column(name = "days_grace_default")
    private Integer daysGraceDefault;

    @Column(name = "activation_date")
    private LocalDate activationDate;

    @Column(name = "cancellation_date")
    private LocalDate cancellationDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sbs_classification_matrix", columnDefinition = "jsonb")
    private List<ClassficationRuleSBS> sbsClassificationMatrix;

}