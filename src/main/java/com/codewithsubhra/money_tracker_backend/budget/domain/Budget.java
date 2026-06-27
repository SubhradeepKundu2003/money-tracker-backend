package com.codewithsubhra.money_tracker_backend.budget.domain;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A per-account spending limit at a given period granularity. The base
 * {@link #limitAmount} is the allowance granted at the start of each period;
 * the running surplus/overspend lives on the {@link BudgetPeriod} rows.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "budgets",
        indexes = @Index(name = "idx_budgets_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(
                name = "uq_budget_account_period",
                columnNames = {"account_id", "period_type"}))
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private BudgetPeriodType periodType;

    /** Base allowance granted at the start of each period. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private boolean active = true;
}
