package com.codewithsubhra.money_tracker_backend.budget.domain;

import com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One concrete budget window. While {@code OPEN}, {@link #spent} is recomputed
 * on read and only finalized when the rollover job closes the period; at close
 * {@link #carriedOut} is written and rolled into the next period's
 * {@link #carriedIn} (positive = surplus, negative = overspend).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "budget_periods",
        indexes = {
                @Index(name = "idx_budget_periods_budget", columnList = "budget_id"),
                @Index(name = "idx_budget_periods_status", columnList = "status, periodEnd")
        })
public class BudgetPeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "budget_id", nullable = false)
    private Budget budget;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    /** Base allowance snapshotted from the budget when this period opened. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitAmount = BigDecimal.ZERO;

    /** Surplus (+) or overspend (-) rolled in from the previous period. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal carriedIn = BigDecimal.ZERO;

    /** Manual money added to this period on top of the allowance. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal topUp = BigDecimal.ZERO;

    /** Expenses recorded against the account in this window. Finalized at close. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal spent = BigDecimal.ZERO;

    /** {@code limitAmount + carriedIn + topUp - spent}; written when closed. */
    @Column(precision = 19, scale = 2)
    private BigDecimal carriedOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BudgetPeriodStatus status = BudgetPeriodStatus.OPEN;

    /** Money still spendable in this window: limit + carry-in + top-ups - spent. */
    public BigDecimal available() {
        return limitAmount.add(carriedIn).add(topUp).subtract(spent);
    }
}
