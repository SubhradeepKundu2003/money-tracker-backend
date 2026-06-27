package com.codewithsubhra.money_tracker_backend.budget.web.dto;

import jakarta.validation.constraints.NotNull;

public record SetActiveRequest(@NotNull Boolean active) {
}
