package com.codewithsubhra.money_tracker_backend.budget.web.dto;

import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID accountId,
        @NotNull BudgetPeriodType periodType,
        @NotNull @DecimalMin(value = "0.01", message = "limitAmount must be greater than zero")
        BigDecimal limitAmount) {
}
