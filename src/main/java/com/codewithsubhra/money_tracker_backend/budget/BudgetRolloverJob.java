package com.codewithsubhra.money_tracker_backend.budget;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily trigger that closes finished budget periods and opens their successors,
 * rolling carry-forward balances. The actual work (and transaction boundary)
 * lives in {@link BudgetService#runRollover(LocalDate)}.
 */
@Component
public class BudgetRolloverJob {

    private static final Logger log = LoggerFactory.getLogger(BudgetRolloverJob.class);

    private final BudgetService budgetService;

    public BudgetRolloverJob(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /** Runs at 00:05 every day, after the previous day's periods have ended. */
    @Scheduled(cron = "0 5 0 * * *")
    public void rollover() {
        int closed = budgetService.runRollover(LocalDate.now());
        if (closed > 0) {
            log.info("Budget rollover closed {} period(s)", closed);
        }
    }
}
