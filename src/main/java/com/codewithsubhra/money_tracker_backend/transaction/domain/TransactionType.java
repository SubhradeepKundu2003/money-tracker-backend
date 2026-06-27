package com.codewithsubhra.money_tracker_backend.transaction.domain;

import java.math.BigDecimal;

public enum TransactionType {
    INCOME,
    EXPENSE;

    /** Signed effect of {@code amount} on an account balance for this type. */
    public BigDecimal signed(BigDecimal amount) {
        return this == INCOME ? amount : amount.negate();
    }
}
