package com.codewithsubhra.money_tracker_backend.account.web.dto;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import java.math.BigDecimal;

public record AccountResponse(
        String id,
        String name,
        String type,
        String currency,
        BigDecimal balance,
        boolean archived) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId().toString(),
                account.getName(),
                account.getType().name(),
                account.getCurrency(),
                account.getBalance(),
                account.isArchived());
    }
}
