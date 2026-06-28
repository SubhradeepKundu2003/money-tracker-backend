package com.codewithsubhra.money_tracker_backend.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for switching the user's base currency. Code is validated against the supported set. */
public record ChangeBaseCurrencyRequest(
        @NotBlank @Size(min = 3, max = 3) String currency) {
}
