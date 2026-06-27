package com.codewithsubhra.money_tracker_backend.security.refresh;

import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user = :user and r.revoked = false")
    int revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :cutoff or r.revoked = true")
    int deleteExpiredOrRevoked(@Param("cutoff") Instant cutoff);
}
