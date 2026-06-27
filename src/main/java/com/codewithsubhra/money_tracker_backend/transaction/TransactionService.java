package com.codewithsubhra.money_tracker_backend.transaction;

import com.codewithsubhra.money_tracker_backend.account.AccountService;
import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.category.CategoryService;
import com.codewithsubhra.money_tracker_backend.category.domain.Category;
import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.transaction.domain.Transaction;
import com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType;
import com.codewithsubhra.money_tracker_backend.transaction.web.dto.TransactionRequest;
import com.codewithsubhra.money_tracker_backend.transaction.web.dto.TransactionSummaryResponse;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final AccountService accountService;
    private final CategoryService categoryService;

    public TransactionService(TransactionRepository transactions, AccountService accountService,
            CategoryService categoryService) {
        this.transactions = transactions;
        this.accountService = accountService;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> search(UUID userId, UUID accountId, TransactionType type,
            LocalDate from, LocalDate to, Pageable pageable) {
        return transactions.search(userId, accountId, type, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID userId, UUID id) {
        return transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction"));
    }

    @Transactional(readOnly = true)
    public TransactionSummaryResponse summary(UUID userId, LocalDate from, LocalDate to) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (TransactionRepository.TypeTotal row : transactions.summarize(userId, from, to)) {
            if (row.getType() == TransactionType.INCOME) {
                income = row.getTotal();
            } else if (row.getType() == TransactionType.EXPENSE) {
                expense = row.getTotal();
            }
        }
        return new TransactionSummaryResponse(income, expense, income.subtract(expense));
    }

    @Transactional
    public Transaction create(User user, TransactionRequest request) {
        Account account = accountService.get(user.getId(), request.accountId());
        Category category = resolveCategory(user.getId(), request.categoryId());

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setAccount(account);
        tx.setCategory(category);
        tx.setType(request.type());
        tx.setAmount(request.amount());
        tx.setOccurredOn(request.occurredOn());
        tx.setNote(request.note());
        Transaction saved = transactions.save(tx);

        accountService.adjustBalance(account, request.type().signed(request.amount()));
        return saved;
    }

    @Transactional
    public Transaction update(UUID userId, UUID id, TransactionRequest request) {
        Transaction tx = get(userId, id);

        // Reverse the previous effect on the (possibly different) original account.
        accountService.adjustBalance(tx.getAccount(), tx.getType().signed(tx.getAmount()).negate());

        Account account = accountService.get(userId, request.accountId());
        tx.setAccount(account);
        tx.setCategory(resolveCategory(userId, request.categoryId()));
        tx.setType(request.type());
        tx.setAmount(request.amount());
        tx.setOccurredOn(request.occurredOn());
        tx.setNote(request.note());
        Transaction saved = transactions.save(tx);

        accountService.adjustBalance(account, request.type().signed(request.amount()));
        return saved;
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Transaction tx = get(userId, id);
        accountService.adjustBalance(tx.getAccount(), tx.getType().signed(tx.getAmount()).negate());
        transactions.delete(tx);
    }

    private Category resolveCategory(UUID userId, UUID categoryId) {
        return categoryId == null ? null : categoryService.get(userId, categoryId);
    }
}
