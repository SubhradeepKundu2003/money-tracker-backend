package com.codewithsubhra.money_tracker_backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} tasks (e.g. the budget rollover job). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
