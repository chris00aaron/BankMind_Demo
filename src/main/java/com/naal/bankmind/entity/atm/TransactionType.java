package com.naal.bankmind.entity.atm;

public enum TransactionType {
    WITHDRAWAL("Retiro"),
    DEPOSIT("Depósito");

    private String name;

    TransactionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
