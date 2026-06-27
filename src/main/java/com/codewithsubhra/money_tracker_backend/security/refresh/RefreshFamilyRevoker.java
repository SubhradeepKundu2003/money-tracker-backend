package com.codewithsubhra.money_tracker_backend.security.refresh;

import com.codewithsubhra.money_tracker_backend.user.domain.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Revokes a user's entire refresh-token family in its <em>own</em> committed
 * transaction. Reuse detection rejects the request by throwing afterwards; that
 * rollback must not undo the revocation, so this runs as {@code REQUIRES_NEW}.
 * It is a separate bean so the proxy actually applies the new propagation.
 */
@Component
public class RefreshFamilyRevoker {

    private final RefreshTokenRepository repository;

    public RefreshFamilyRevoker(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAll(User user) {
        repository.revokeAllForUser(user);
    }
}
