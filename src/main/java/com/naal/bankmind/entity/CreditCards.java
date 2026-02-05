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

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Helper method to get masked card number for emails
    public String getMaskedCardNumber() {
        if (ccNum == null)
            return "****";
        String cardStr = String.valueOf(ccNum);
        if (cardStr.length() < 4)
            return "****";
        return "**** " + cardStr.substring(cardStr.length() - 4);
    }
}
