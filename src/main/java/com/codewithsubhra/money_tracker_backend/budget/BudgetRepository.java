package com.codewithsubhra.money_tracker_backend.budget;

import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    // Fetch-join the account so DTO mapping (which reads account.name) happens
    // against an initialized association — the response is built after the
    // service transaction closes, so a lazy account would fail.
    @Query("select b from Budget b join fetch b.account"
            + " where b.user.id = :userId order by b.createdAt")
    List<Budget> findByUserIdOrderByCreatedAt(@Param("userId") UUID userId);

    @Query("select b from Budget b join fetch b.account"
            + " where b.id = :id and b.user.id = :userId")
    Optional<Budget> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByAccountIdAndPeriodType(UUID accountId, BudgetPeriodType periodType);
}
