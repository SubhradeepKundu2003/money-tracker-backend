package com.codewithsubhra.money_tracker_backend.budget;

import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriod;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, UUID> {

    Optional<BudgetPeriod> findByBudgetIdAndStatus(UUID budgetId, BudgetPeriodStatus status);

    List<BudgetPeriod> findByBudgetIdOrderByPeriodStartDesc(UUID budgetId);

    /** Open periods whose window has already ended — candidates for the rollover job. */
    @Query("""
            select p from BudgetPeriod p
            where p.status = com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodStatus.OPEN
              and p.periodEnd < :today
            """)
    List<BudgetPeriod> findOpenPeriodsEndedBefore(@Param("today") LocalDate today);
}
