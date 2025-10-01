package com.checkbook.api.entity;

public enum AccountType {
    CHECKING("Checking"),
    SAVINGS("Savings"),
    CREDIT_CARD("Credit Card"),
    INVESTMENT("Investment"),
    CASH("Cash"),
    OTHER("Other");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}