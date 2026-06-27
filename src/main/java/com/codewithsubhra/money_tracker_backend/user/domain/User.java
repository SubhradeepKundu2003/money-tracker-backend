package com.codewithsubhra.money_tracker_backend.user.domain;

import com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email", unique = true))
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash — never the plaintext password. */
    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    /** Default currency code (ISO 4217) used when creating accounts/transactions. */
    @Column(nullable = false, length = 3)
    private String baseCurrency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;
}
