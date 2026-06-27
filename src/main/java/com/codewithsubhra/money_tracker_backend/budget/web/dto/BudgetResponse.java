package com.codewithsubhra.money_tracker_backend.budget.web.dto;

import com.codewithsubhra.money_tracker_backend.budget.BudgetView;
import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetResponse(
        String id,
        String accountId,
        String accountName,
        String periodType,
        BigDecimal limitAmount,
        boolean active,
        CurrentPeriod currentPeriod) {

    public record CurrentPeriod(
            String id,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal limitAmount,
            BigDecimal carriedIn,
            BigDecimal topUp,
            BigDecimal spent,
            BigDecimal available) {
    }

    public static BudgetResponse from(BudgetView view) {
        Budget b = view.budget();
        BudgetPeriod p = view.currentPeriod();
        CurrentPeriod current = new CurrentPeriod(
                p.getId().toString(),
                p.getPeriodStart(),
                p.getPeriodEnd(),
                p.getLimitAmount(),
                p.getCarriedIn(),
                p.getTopUp(),
                view.liveSpent(),
                view.available());
        return new BudgetResponse(
                b.getId().toString(),
                b.getAccount().getId().toString(),
                b.getAccount().getName(),
                b.getPeriodType().name(),
                b.getLimitAmount(),
                b.isActive(),
                current);
    }
}
