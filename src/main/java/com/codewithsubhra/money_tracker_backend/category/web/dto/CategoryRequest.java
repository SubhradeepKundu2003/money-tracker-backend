package com.codewithsubhra.money_tracker_backend.category.web.dto;

import com.codewithsubhra.money_tracker_backend.category.domain.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull CategoryType type,
        @Size(max = 40) String icon,
        @Size(max = 16) String color) {
}
