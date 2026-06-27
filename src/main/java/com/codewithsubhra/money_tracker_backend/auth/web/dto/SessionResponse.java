package com.codewithsubhra.money_tracker_backend.auth.web.dto;

import java.time.Instant;

/**
 * The login/register/refresh payload returned to the client. Only the opaque
 * tokens are exposed — never the internal JWT.
 *
 * @param sessionToken     short-lived bearer token for API calls
 * @param expiresAt        when {@code sessionToken} expires
 * @param refreshToken     long-lived, single-use token to obtain a new session
 * @param refreshExpiresAt absolute expiry of the refresh-token family
 * @param user             the authenticated user (null on refresh)
 */
public record SessionResponse(
        String sessionToken,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt,
        UserResponse user) {
}
