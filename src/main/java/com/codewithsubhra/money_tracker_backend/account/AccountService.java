package com.codewithsubhra.money_tracker_backend.account;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.account.web.dto.AccountRequest;
import com.codewithsubhra.money_tracker_backend.common.exception.ResourceNotFoundException;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<Account> list(UUID userId) {
        return accounts.findByUserIdOrderByCreatedAt(userId);
    }

    @Transactional(readOnly = true)
    public Account get(UUID userId, UUID id) {
        return accounts.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account"));
    }

    @Transactional
    public Account create(User user, AccountRequest request) {
        Account account = new Account();
        account.setUser(user);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setCurrency(resolveCurrency(request.currency(), user.getBaseCurrency()));
        account.setBalance(request.openingBalance() != null ? request.openingBalance() : BigDecimal.ZERO);
        return accounts.save(account);
    }

    @Transactional
    public Account update(UUID userId, UUID id, AccountRequest request) {
        Account account = get(userId, id);
        account.setName(request.name().trim());
        account.setType(request.type());
        account.setCurrency(resolveCurrency(request.currency(), account.getCurrency()));
        return accounts.save(account);
    }

    @Transactional
    public Account setArchived(UUID userId, UUID id, boolean archived) {
        Account account = get(userId, id);
        account.setArchived(archived);
        return accounts.save(account);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        accounts.delete(get(userId, id));
    }

    /** Applies a signed delta to the running balance. Used by the transaction module. */
    @Transactional
    public void adjustBalance(Account account, BigDecimal delta) {
        account.setBalance(account.getBalance().add(delta));
        accounts.save(account);
    }

    private String resolveCurrency(String requested, String fallback) {
        return (requested == null || requested.isBlank())
                ? fallback
                : requested.trim().toUpperCase();
    }
}
