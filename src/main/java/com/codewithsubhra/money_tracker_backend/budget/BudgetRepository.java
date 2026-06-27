package com.codewithsubhra.money_tracker_backend.budget;

import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdOrderByCreatedAt(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByAccountIdAndPeriodType(UUID accountId, BudgetPeriodType periodType);
}
