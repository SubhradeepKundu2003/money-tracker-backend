package com.codewithsubhra.money_tracker_backend.security.session;

import com.codewithsubhra.money_tracker_backend.security.AuthPrincipal;
import com.codewithsubhra.money_tracker_backend.security.OpaqueTokens;
import com.codewithsubhra.money_tracker_backend.security.SecurityProperties;
import com.codewithsubhra.money_tracker_backend.security.jwt.JwtService;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the lifecycle of opaque sessions: it mints an internal JWT, persists it
 * alongside a hash of a freshly generated random token, and resolves an
 * incoming opaque token back into a verified {@link AuthPrincipal}.
 */
@Service
public class SessionService {

    private final SessionRepository sessions;
    private final JwtService jwtService;
    private final long ttlMinutes;

    public SessionService(SessionRepository sessions, JwtService jwtService, SecurityProperties properties) {
        this.sessions = sessions;
        this.jwtService = jwtService;
        this.ttlMinutes = properties.sessionTtlMinutes();
    }

    /** Creates a new session for the user and returns the opaque token to hand to the client. */
    @Transactional
    public IssuedSession createSession(User user, String userAgent) {
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        String jwt = jwtService.issue(user.getId(), user.getEmail(), user.getRole().name(), expiresAt);

        String rawToken = OpaqueTokens.generate();

        Session session = new Session();
        session.setUser(user);
        session.setTokenHash(OpaqueTokens.sha256(rawToken));
        session.setJwt(jwt);
        session.setExpiresAt(expiresAt);
        session.setUserAgent(truncate(userAgent));
        session.setLastUsedAt(Instant.now());
        sessions.save(session);

        return new IssuedSession(rawToken, expiresAt);
    }

    /** Resolves an opaque token into a principal, or empty if it is invalid/expired/revoked. */
    @Transactional
    public Optional<AuthPrincipal> authenticate(String rawToken) {
        Optional<Session> found = sessions.findByTokenHash(OpaqueTokens.sha256(rawToken));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Session session = found.get();
        Instant now = Instant.now();
        if (!session.isActive(now)) {
            return Optional.empty();
        }
        Claims claims;
        try {
            claims = jwtService.verify(session.getJwt());
        } catch (JwtException ex) {
            // Internal token tampered with or expired — treat as invalid.
            session.setRevoked(true);
            return Optional.empty();
        }
        session.setLastUsedAt(now);
        return Optional.of(new AuthPrincipal(
                UUID.fromString(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("role", String.class)));
    }

    @Transactional
    public void revoke(String rawToken) {
        sessions.findByTokenHash(OpaqueTokens.sha256(rawToken)).ifPresent(s -> s.setRevoked(true));
    }

    @Transactional
    public void revokeAll(User user) {
        sessions.revokeAllForUser(user);
    }

    private String truncate(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}
