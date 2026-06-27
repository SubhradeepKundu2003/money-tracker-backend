package com.codewithsubhra.money_tracker_backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables population of {@code @CreatedDate}/{@code @LastModifiedDate} fields. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
