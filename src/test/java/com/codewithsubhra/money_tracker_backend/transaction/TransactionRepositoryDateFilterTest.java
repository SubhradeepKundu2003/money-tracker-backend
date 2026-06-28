package com.codewithsubhra.money_tracker_backend.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import com.codewithsubhra.money_tracker_backend.account.domain.Account;
import com.codewithsubhra.money_tracker_backend.account.domain.AccountType;
import com.codewithsubhra.money_tracker_backend.transaction.domain.Transaction;
import com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType;
import com.codewithsubhra.money_tracker_backend.user.domain.User;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Regression test for the date-filtered transaction queries. These run against
 * a real PostgreSQL (via Testcontainers) on purpose: the original
 * {@code (:from is null or t.occurredOn >= :from)} predicate emitted a bare
 * {@code ? IS NULL} placeholder that PostgreSQL rejected with
 * "could not determine data type of parameter" — a failure H2 does NOT
 * reproduce. The {@code cast(:from as date)} fix is what makes these pass.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class TransactionRepositoryDateFilterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionRepository transactions;

    @Autowired
    private TestEntityManager em;

    private User user;

    @BeforeEach
    void seed() {
        user = persist(newUser("filter-test@example.com"));
        Account account = persist(newAccount(user, "Cash"));

        // Two transactions in June 2026, one in May 2026.
        persist(newTx(user, account, TransactionType.INCOME, "1000.00", LocalDate.of(2026, 6, 15)));
        persist(newTx(user, account, TransactionType.EXPENSE, "200.00", LocalDate.of(2026, 6, 20)));
        persist(newTx(user, account, TransactionType.INCOME, "500.00", LocalDate.of(2026, 5, 15)));
        em.flush();
    }

    @Test
    void summarize_withDateRange_filtersToThatRange() {
        List<TransactionRepository.TypeTotal> rows =
                transactions.summarize(user.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(total(rows, TransactionType.INCOME)).isEqualByComparingTo("1000.00");
        assertThat(total(rows, TransactionType.EXPENSE)).isEqualByComparingTo("200.00");
    }

    @Test
    void summarize_withNullDates_returnsAllTime() {
        List<TransactionRepository.TypeTotal> rows =
                transactions.summarize(user.getId(), null, null);

        assertThat(total(rows, TransactionType.INCOME)).isEqualByComparingTo("1500.00");
        assertThat(total(rows, TransactionType.EXPENSE)).isEqualByComparingTo("200.00");
    }

    @Test
    void search_withDateRange_filtersToThatRange() {
        Page<Transaction> page = transactions.search(
                user.getId(), null, null,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30),
                PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .allSatisfy(t -> assertThat(t.getOccurredOn().getMonthValue()).isEqualTo(6));
    }

    @Test
    void search_withNullDates_returnsAll() {
        Page<Transaction> page = transactions.search(
                user.getId(), null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    // --- helpers ---

    private BigDecimal total(List<TransactionRepository.TypeTotal> rows, TransactionType type) {
        return rows.stream()
                .filter(r -> r.getType() == type)
                .map(TransactionRepository.TypeTotal::getTotal)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private <T> T persist(T entity) {
        return em.persist(entity);
    }

    private static User newUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash("not-a-real-hash");
        u.setDisplayName("Filter Test");
        stamp(u);
        return u;
    }

    private static Account newAccount(User user, String name) {
        Account a = new Account();
        a.setUser(user);
        a.setName(name);
        a.setType(AccountType.CASH);
        stamp(a);
        return a;
    }

    private static Transaction newTx(User user, Account account, TransactionType type,
            String amount, LocalDate occurredOn) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setAccount(account);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        t.setOccurredOn(occurredOn);
        stamp(t);
        return t;
    }

    // Audit columns are non-null; set them explicitly so the test does not
    // depend on JPA auditing being active in the @DataJpaTest slice.
    private static void stamp(
            com.codewithsubhra.money_tracker_backend.common.domain.BaseEntity e) {
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
    }
}
