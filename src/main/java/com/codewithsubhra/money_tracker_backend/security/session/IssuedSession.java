package com.codewithsubhra.money_tracker_backend.security.session;

import java.time.Instant;

/**
 * Result of creating a session. {@code token} is the opaque value that — and
 * the only value that — is returned to the client.
 */
public record IssuedSession(String token, Instant expiresAt) {
}
