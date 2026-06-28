package com.codewithsubhra.money_tracker_backend.user;

import com.codewithsubhra.money_tracker_backend.account.AccountRepository;
import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.budget.BudgetPeriodRepository;
import com.codewithsubhra.money_tracker_backend.budget.BudgetRepository;
import com.codewithsubhra.money_tracker_backend.budget.domain.Budget;
import com.codewithsubhra.money_tracker_backend.budget.domain.BudgetPeriod;
import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.currency.CurrencyService;
import com.codewithsubhra.money_tracker_backend.transaction.TransactionRepository;
import com.codewithsubhra.money_tracker_backend.transaction.domain.Transaction;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes a user's base currency and re-denominates all of their money into it
 * at the current market rate: every account's balance and currency, all of that
 * account's transaction amounts, and any budgets/periods on it.
 *
 * <p>Because conversion is a single multiply, per-field rounding can drift by a
 * cent from the converted balance, which is acceptable for a personal tracker.
 * The (cached) FX lookups happen inside the transaction; account counts are tiny
 * here, so the open-transaction window is negligible.
 */
@Service
public class BaseCurrencyService {

    private final UserRepository users;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final BudgetPeriodRepository budgetPeriods;
    private final CurrencyService currency;

    public BaseCurrencyService(UserRepository users, AccountRepository accounts,
            TransactionRepository transactions, BudgetRepository budgets,
            BudgetPeriodRepository budgetPeriods, CurrencyService currency) {
        this.users = users;
        this.accounts = accounts;
        this.transactions = transactions;
        this.budgets = budgets;
        this.budgetPeriods = budgetPeriods;
        this.currency = currency;
    }

    @Transactional
    public User changeBaseCurrency(UUID userId, String requestedCurrency) {
        String target = currency.requireSupported(requestedCurrency);
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User"));

        List<Account> userAccounts = accounts.findByUserIdOrderByCreatedAt(userId);

        // Snapshot each account's original currency before we mutate anything,
        // so budget conversions use the source currency regardless of order.
        Map<UUID, String> originalCurrency = new HashMap<>();
        for (Account account : userAccounts) {
            originalCurrency.put(account.getId(), account.getCurrency());
        }

        for (Account account : userAccounts) {
            String src = originalCurrency.get(account.getId());
            account.setBalance(currency.convert(account.getBalance(), src, target));
            account.setCurrency(target);
            for (Transaction tx : transactions.findByAccountId(account.getId())) {
                tx.setAmount(currency.convert(tx.getAmount(), src, target));
            }
        }

        for (Budget budget : budgets.findByUserIdOrderByCreatedAt(userId)) {
            String src = originalCurrency.getOrDefault(budget.getAccount().getId(), target);
            budget.setLimitAmount(currency.convert(budget.getLimitAmount(), src, target));
            for (BudgetPeriod period : budgetPeriods.findByBudgetIdOrderByPeriodStartDesc(budget.getId())) {
                period.setLimitAmount(currency.convert(period.getLimitAmount(), src, target));
                period.setCarriedIn(currency.convert(period.getCarriedIn(), src, target));
                period.setTopUp(currency.convert(period.getTopUp(), src, target));
                period.setSpent(currency.convert(period.getSpent(), src, target));
                period.setCarriedOut(currency.convert(period.getCarriedOut(), src, target));
            }
        }

        user.setBaseCurrency(target);
        return user;
    }
}
