package com.codewithsubhra.money_tracker_backend.transaction.web.dto;

import com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull UUID accountId,
        UUID categoryId,
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero") BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @Size(max = 255) String note) {
}
