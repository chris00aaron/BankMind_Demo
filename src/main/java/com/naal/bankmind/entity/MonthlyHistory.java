package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "monthly_history")
public class MonthlyHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne
    @JoinColumn(name = "record_id")
    private AccountDetails accountDetails;

    @Column(name = "monthly_period")
    private LocalDate monthlyPeriod;

    @Column(name = "pay_x")
    private Integer payX;

    @Column(name = "bill_amt_X", precision = 15, scale = 2)
    private BigDecimal billAmtX;

    @Column(name = "pay_amt_X", precision = 15, scale = 2)
    private BigDecimal payAmtX;

    @Column(name = "did_customer_pay")
    private Boolean didCustomerPay;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "actual_payment_date")
    private LocalDate actualPaymentDate;

}
