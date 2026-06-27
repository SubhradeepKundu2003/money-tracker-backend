package com.codewithsubhra.money_tracker_backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Strongly-typed binding of the {@code app.security.*} configuration. */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        String jwtSecret,
        long sessionTtlMinutes,
        long refreshTtlDays,
        String issuer) {

    public SecurityProperties {
        if (sessionTtlMinutes <= 0) {
            sessionTtlMinutes = 60;
        }
        if (refreshTtlDays <= 0) {
            refreshTtlDays = 30;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "money-tracker";
        }
    }
}
