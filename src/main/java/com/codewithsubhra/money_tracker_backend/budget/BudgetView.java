package com.codewithsubhra.money_tracker_backend.budget;

import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriod;
import java.math.BigDecimal;

/**
 * A budget paired with its current open period, where {@code liveSpent} is the
 * up-to-the-moment expense total (the period's stored {@code spent} is only
 * finalized when the rollover job closes it).
 */
public record BudgetView(Budget budget, BudgetPeriod currentPeriod, BigDecimal liveSpent) {

    /** Spendable now: period allowance + carry-in + top-ups - live spend. */
    public BigDecimal available() {
        return currentPeriod.getLimitAmount()
                .add(currentPeriod.getCarriedIn())
                .add(currentPeriod.getTopUp())
                .subtract(liveSpent);
    }
}
