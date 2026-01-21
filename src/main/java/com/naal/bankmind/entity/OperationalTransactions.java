package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "operational_transactions")
public class OperationalTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaction")
    private Long idTransaction;

    @ManyToOne
    @JoinColumn(name = "cc_num")
    private CreditCards creditCard;

    @Column(name = "trans_num", length = 100, unique = true)
    private String transNum;

    @Column(name = "trans_date_time")
    private LocalDateTime transDateTime;

    @Column(name = "amt", precision = 15, scale = 2)
    private BigDecimal amt;

    @Column(name = "merchant", length = 150)
    private String merchant;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "merch_lat")
    private Double merchLat;

    @Column(name = "merch_long")
    private Double merchLong;

    @Column(name = "unix_time")
    private Long unixTime;

    @Column(name = "is_fraud_ground_truth")
    private Integer isFraudGroundTruth;
}
