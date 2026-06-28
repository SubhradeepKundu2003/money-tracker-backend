package com.codewithsubhra.money_tracker_backend.currency;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the foreign-exchange provider (frankfurter.app by default).
 * Rates are cached in-memory for {@code cacheTtlMinutes} to limit upstream calls.
 */
@ConfigurationProperties(prefix = "app.fx")
public record FxProperties(String baseUrl, long cacheTtlMinutes) {

    public FxProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.frankfurter.dev/v1";
        }
        // Strip a trailing slash so path concatenation is predictable.
        baseUrl = baseUrl.replaceAll("/+$", "");
        if (cacheTtlMinutes <= 0) {
            cacheTtlMinutes = 360; // 6 hours — frankfurter publishes once per working day
        }
    }
}
