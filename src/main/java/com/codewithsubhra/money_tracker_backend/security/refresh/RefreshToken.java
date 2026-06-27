package com.codewithsubhra.money_tracker_backend.security.refresh;

import com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A long-lived, single-use refresh credential. The client holds the raw token;
 * we persist only its SHA-256 hash. On use it is rotated: this row is revoked
 * and a new one issued, with {@link #replacedByHash} linking the chain so a
 * replayed (stolen) token can be detected.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_tokens",
        indexes = @Index(name = "idx_refresh_token_hash", columnList = "tokenHash", unique = true))
public class RefreshToken extends BaseEntity {

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    /** Hash of the token that superseded this one during rotation, if any. */
    @Column(length = 64)
    private String replacedByHash;

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }
}
