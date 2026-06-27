package com.codewithsubhra.money_tracker_backend.security.refresh;

import com.codewithsubhra.money_tracker_backend.common.exception.UnauthorizedException;
import com.codewithsubhra.money_tracker_backend.security.OpaqueTokens;
import com.codewithsubhra.money_tracker_backend.security.SecurityProperties;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues and rotates refresh tokens. Rotation is single-use with reuse
 * detection: presenting an already-revoked token is treated as theft and
 * revokes the user's entire refresh-token family.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final RefreshFamilyRevoker familyRevoker;
    private final long ttlDays;

    public RefreshTokenService(RefreshTokenRepository repository, RefreshFamilyRevoker familyRevoker,
            SecurityProperties properties) {
        this.repository = repository;
        this.familyRevoker = familyRevoker;
        this.ttlDays = properties.refreshTtlDays();
    }

    /** Mints a brand-new refresh token for a fresh login/registration. */
    @Transactional
    public IssuedRefreshToken issue(User user) {
        return persist(user, null);
    }

    /** The user behind a refresh token, plus its freshly rotated replacement. */
    public record Rotation(User user, IssuedRefreshToken issued) {
    }

    /**
     * Validates and rotates a refresh token.
     *
     * <p>Reuse detection revokes the whole family via {@link RefreshFamilyRevoker}
     * (a separate, independently-committed transaction) before throwing, so the
     * rejection's rollback cannot resurrect the stolen family.
     *
     * @throws UnauthorizedException if the token is unknown, expired, or replayed
     */
    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(OpaqueTokens.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        Instant now = Instant.now();

        if (current.isRevoked()) {
            // A previously-rotated token is being replayed — likely stolen.
            log.warn("Refresh token reuse detected for user {}; revoking all sessions", current.getUser().getId());
            familyRevoker.revokeAll(current.getUser());
            throw new UnauthorizedException("Refresh token has already been used");
        }
        if (current.getExpiresAt().isBefore(now)) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = current.getUser();
        IssuedRefreshToken next = persist(user, current);
        current.setRevoked(true);
        current.setReplacedByHash(OpaqueTokens.sha256(next.token()));
        return new Rotation(user, next);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(OpaqueTokens.sha256(rawToken))
                .ifPresent(token -> token.setRevoked(true));
    }

    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllForUser(user);
    }

    private IssuedRefreshToken persist(User user, RefreshToken previous) {
        String raw = OpaqueTokens.generate();
        Instant expiresAt = previous != null
                ? previous.getExpiresAt() // keep the family's original expiry on rotation
                : Instant.now().plus(ttlDays, ChronoUnit.DAYS);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(OpaqueTokens.sha256(raw));
        token.setExpiresAt(expiresAt);
        repository.save(token);

        return new IssuedRefreshToken(raw, expiresAt);
    }
}
