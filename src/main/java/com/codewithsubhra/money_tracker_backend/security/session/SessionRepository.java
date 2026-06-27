package com.codewithsubhra.money_tracker_backend.security.session;

import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update Session s set s.revoked = true where s.user = :user and s.revoked = false")
    int revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("delete from Session s where s.expiresAt < :cutoff or s.revoked = true")
    int deleteExpiredOrRevoked(@Param("cutoff") Instant cutoff);
}
