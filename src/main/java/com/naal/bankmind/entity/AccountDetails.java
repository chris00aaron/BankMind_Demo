package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "account_details")
public class AccountDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @Column(name = "limit_bal", precision = 15, scale = 2)
    private BigDecimal limitBal;

    @Column(name = "num_of_products")
    private Integer numOfProducts;

    @Column(name = "balance", precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "has_cr_card")
    private Boolean hasCrCard;

    @Column(name = "estimated_salary", precision = 15, scale = 2)
    private BigDecimal estimatedSalary;

    @Column(name = "tenure")
    private Integer tenure;

    @Column(name = "credit_score")
    private Integer creditScore;

    @Column(name = "exited")
    private Boolean exited;

    @Column(name = "is_active_member")
    private Boolean isActiveMember;
}
