package com.codewithsubhra.money_tracker_backend.auth.web.dto;

import com.codewithsubhra.money_tracker_backend.user.domain.User;

public record UserResponse(
        String id,
        String email,
        String displayName,
        String baseCurrency,
        String role) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBaseCurrency(),
                user.getRole().name());
    }
}
