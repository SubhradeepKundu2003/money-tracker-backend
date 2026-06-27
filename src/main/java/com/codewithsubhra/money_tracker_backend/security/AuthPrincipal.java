package com.codewithsubhra.money_tracker_backend.security;

import java.util.UUID;

/**
 * Lightweight authenticated principal exposed to controllers via
 * {@code @AuthenticationPrincipal}. Derived from the verified internal JWT.
 */
public record AuthPrincipal(UUID userId, String email, String role) {
}
