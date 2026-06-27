package com.codewithsubhra.money_tracker_backend.account.web.dto;

import com.codewithsubhra.money_tracker_backend.account.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull AccountType type,
        @Size(min = 3, max = 3) String currency,
        BigDecimal openingBalance) {
}
