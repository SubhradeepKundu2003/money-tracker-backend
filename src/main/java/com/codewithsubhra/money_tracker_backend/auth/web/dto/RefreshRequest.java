package com.codewithsubhra.money_tracker_backend.auth.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken) {
}
