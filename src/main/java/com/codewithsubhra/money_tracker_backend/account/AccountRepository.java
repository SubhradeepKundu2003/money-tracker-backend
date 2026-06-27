package com.codewithsubhra.money_tracker_backend.account;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUserIdOrderByCreatedAt(UUID userId);

    Optional<Account> findByIdAndUserId(UUID id, UUID userId);
}
