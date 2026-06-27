package com.codewithsubhra.money_tracker_backend.security.refresh;

import java.time.Instant;

/** The raw refresh token (shown to the client once) plus its expiry. */
public record IssuedRefreshToken(String token, Instant expiresAt) {
}
