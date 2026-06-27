package com.codewithsubhra.money_tracker_backend.transaction.web.dto;

import java.math.BigDecimal;

public record TransactionSummaryResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal net) {
}
