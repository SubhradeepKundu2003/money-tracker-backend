package com.codewithsubhra.money_tracker_backend.transaction.domain;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.category.domain.Category;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_user_date", columnList = "user_id, occurredOn"),
        @Index(name = "idx_tx_account", columnList = "account_id")
})
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    /** Always stored as a positive magnitude; direction comes from {@link #type}. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate occurredOn;

    @Column(length = 255)
    private String note;
}
