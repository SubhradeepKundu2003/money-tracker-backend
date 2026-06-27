package com.codewithsubhra.money_tracker_backend.budget.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Granularity of a budget. Each value can resolve the calendar window
 * (inclusive {@code [start, end]}) that contains a given date, plus the start
 * of the following window — the two operations the rollover job needs.
 */
public enum BudgetPeriodType {

    DAILY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date;
        }

        @Override
        public LocalDate periodEnd(LocalDate date) {
            return date;
        }
    },

    WEEKLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        @Override
        public LocalDate periodEnd(LocalDate date) {
            return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }
    },

    MONTHLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.withDayOfMonth(1);
        }

        @Override
        public LocalDate periodEnd(LocalDate date) {
            return date.with(TemporalAdjusters.lastDayOfMonth());
        }
    },

    YEARLY {
        @Override
        public LocalDate periodStart(LocalDate date) {
            return date.withDayOfYear(1);
        }

        @Override
        public LocalDate periodEnd(LocalDate date) {
            return date.with(TemporalAdjusters.lastDayOfYear());
        }
    };

    /** Inclusive first day of the window containing {@code date}. */
    public abstract LocalDate periodStart(LocalDate date);

    /** Inclusive last day of the window containing {@code date}. */
    public abstract LocalDate periodEnd(LocalDate date);

    /** First day of the window immediately following the one ending on {@code periodEnd}. */
    public LocalDate nextStart(LocalDate periodEnd) {
        return periodEnd.plusDays(1);
    }
}
