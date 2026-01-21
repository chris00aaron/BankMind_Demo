package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "operations_atms")
public class OperationsAtms {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_operation_atms")
    private Long idOperationAtms;

    @ManyToOne
    @JoinColumn(name = "id_atm")
    private Atms atm;

    @ManyToOne
    @JoinColumn(name = "date_operation")
    private DateTime dateOperation;

    @ManyToOne
    @JoinColumn(name = "id_clima")
    private Clima clima;

    @Column(name = "amount_withdrawn", precision = 15, scale = 3)
    private BigDecimal amountWithdrawn;

    @Column(name = "amount_deposited", precision = 15, scale = 3)
    private BigDecimal amountDeposited;

    @Column(name = "balance", precision = 15, scale = 3)
    private BigDecimal balance;
}
