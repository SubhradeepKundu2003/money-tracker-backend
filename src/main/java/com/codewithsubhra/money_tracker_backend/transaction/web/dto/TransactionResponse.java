package com.codewithsubhra.money_tracker_backend.transaction.web.dto;

import com.codewithsubhra.money_tracker_backend.transaction.domain.Transaction;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        String id,
        String accountId,
        String accountName,
        String categoryId,
        String categoryName,
        String type,
        BigDecimal amount,
        LocalDate occurredOn,
        String note) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId().toString(),
                t.getAccount().getId().toString(),
                t.getAccount().getName(),
                t.getCategory() != null ? t.getCategory().getId().toString() : null,
                t.getCategory() != null ? t.getCategory().getName() : null,
                t.getType().name(),
                t.getAmount(),
                t.getOccurredOn(),
                t.getNote());
    }
}
