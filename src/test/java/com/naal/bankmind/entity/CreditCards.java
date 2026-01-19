package com.naal.bankmind.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "credit_cards")
public class CreditCards {

    @Id
    @Column(name = "cc_num")
    private Long ccNum;

    @ManyToOne
    @JoinColumn(name = "id_customer")
    private Customer customer;
}
