package com.codewithsubhra.money_tracker_backend.security.session;

import com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Server-side session. The client only knows {@code tokenHash}'s pre-image (the
 * opaque session token); the signed JWT is kept here and never exposed.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "sessions", indexes = @Index(name = "idx_sessions_token_hash", columnList = "tokenHash", unique = true))
public class Session extends BaseEntity {

    /** SHA-256 hash of the opaque session token presented by the client. */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The internal signed JWT — server-side only. */
    @Lob
    @Column(nullable = false)
    private String jwt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    private Instant lastUsedAt;

    private String userAgent;

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }
}
