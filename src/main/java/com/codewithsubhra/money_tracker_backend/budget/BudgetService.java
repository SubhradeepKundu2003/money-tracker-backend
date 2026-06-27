package com.codewithsubhra.money_tracker_backend.budget;

import com.codewithsubhra.money_tracker_backend.account.AccountService;
import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriod;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriodStatus;
import com.codewithsubhra.money_tracker_backend.budget.web.dto.BudgetRequest;
import com.codewithsubhra.money_tracker_backend.common.exception.BadRequestException;
import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.transaction.TransactionRepository;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetRepository budgets;
    private final BudgetPeriodRepository periods;
    private final TransactionRepository transactions;
    private final AccountService accountService;

    public BudgetService(BudgetRepository budgets, BudgetPeriodRepository periods,
            TransactionRepository transactions, AccountService accountService) {
        this.budgets = budgets;
        this.periods = periods;
        this.transactions = transactions;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<BudgetView> list(UUID userId) {
        return budgets.findByUserIdOrderByCreatedAt(userId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetView get(UUID userId, UUID id) {
        return toView(load(userId, id));
    }

    @Transactional
    public BudgetView create(User user, BudgetRequest request) {
        Account account = accountService.get(user.getId(), request.accountId());
        if (budgets.existsByAccountIdAndPeriodType(account.getId(), request.periodType())) {
            throw new BadRequestException(
                    "A " + request.periodType() + " budget already exists for this account");
        }

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setAccount(account);
        budget.setPeriodType(request.periodType());
        budget.setLimitAmount(request.limitAmount());
        Budget saved = budgets.save(budget);

        openPeriod(saved, LocalDate.now(), BigDecimal.ZERO);
        return toView(saved);
    }

    @Transactional
    public BudgetView update(UUID userId, UUID id, BudgetRequest request) {
        Budget budget = load(userId, id);
        // Account and period type are structural; only the allowance/active state change.
        budget.setLimitAmount(request.limitAmount());
        return toView(budgets.save(budget));
    }

    @Transactional
    public BudgetView setActive(UUID userId, UUID id, boolean active) {
        Budget budget = load(userId, id);
        budget.setActive(active);
        return toView(budgets.save(budget));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Budget budget = load(userId, id);
        periods.deleteAll(periods.findByBudgetIdOrderByPeriodStartDesc(id));
        budgets.delete(budget);
    }

    /** Adds money to the current open period's top-up pool. */
    @Transactional
    public BudgetView topUp(UUID userId, UUID id, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Top-up amount must be greater than zero");
        }
        Budget budget = load(userId, id);
        BudgetPeriod period = currentPeriod(budget);
        period.setTopUp(period.getTopUp().add(amount));
        periods.save(period);
        return toView(budget);
    }

    /**
     * Closes every open period whose window has ended and opens its successor,
     * rolling the surplus/overspend forward. Catches up multiple elapsed periods
     * (e.g. after downtime). Inactive budgets are closed without reopening.
     */
    @Transactional
    public int runRollover(LocalDate today) {
        int closed = 0;
        for (BudgetPeriod ended : periods.findOpenPeriodsEndedBefore(today)) {
            BudgetPeriod current = ended;
            while (current.getStatus() == BudgetPeriodStatus.OPEN
                    && current.getPeriodEnd().isBefore(today)) {
                Budget budget = current.getBudget();

                BigDecimal spent = transactions.sumExpense(
                        budget.getAccount().getId(), current.getPeriodStart(), current.getPeriodEnd());
                current.setSpent(spent);
                BigDecimal carriedOut = current.available();
                current.setCarriedOut(carriedOut);
                current.setStatus(BudgetPeriodStatus.CLOSED);
                periods.save(current);
                closed++;

                if (!budget.isActive()) {
                    break;
                }
                LocalDate nextDate = budget.getPeriodType().nextStart(current.getPeriodEnd());
                current = openPeriod(budget, nextDate, carriedOut);
            }
        }
        return closed;
    }

    private Budget load(UUID userId, UUID id) {
        return budgets.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget"));
    }

    /** Finds the open period, lazily opening one for today if none exists. */
    private BudgetPeriod currentPeriod(Budget budget) {
        return periods.findByBudgetIdAndStatus(budget.getId(), BudgetPeriodStatus.OPEN)
                .orElseGet(() -> openPeriod(budget, LocalDate.now(), BigDecimal.ZERO));
    }

    private BudgetPeriod openPeriod(Budget budget, LocalDate onDate, BigDecimal carriedIn) {
        BudgetPeriod period = new BudgetPeriod();
        period.setBudget(budget);
        period.setPeriodStart(budget.getPeriodType().periodStart(onDate));
        period.setPeriodEnd(budget.getPeriodType().periodEnd(onDate));
        period.setLimitAmount(budget.getLimitAmount());
        period.setCarriedIn(carriedIn);
        period.setStatus(BudgetPeriodStatus.OPEN);
        return periods.save(period);
    }

    private BudgetView toView(Budget budget) {
        BudgetPeriod period = currentPeriod(budget);
        BigDecimal liveSpent = transactions.sumExpense(
                budget.getAccount().getId(), period.getPeriodStart(), period.getPeriodEnd());
        return new BudgetView(budget, period, liveSpent);
    }
}
