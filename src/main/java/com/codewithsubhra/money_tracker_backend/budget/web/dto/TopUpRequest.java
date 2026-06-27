package com.codewithsubhra.money_tracker_backend.budget.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TopUpRequest(
        @NotNull @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount) {
}
