package com.codewithsubhra.money_tracker_backend.transaction;

import com.codewithsubhra.money_tracker_backend.transaction.domain.Transaction;
import com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** All transactions on an account — used by the base-currency conversion. */
    List<Transaction> findByAccountId(UUID accountId);

    // Fetch-join account/category so DTO mapping (which reads their names) runs
    // against initialized associations — responses are built after the service
    // transaction closes, so lazy proxies would fail with no open session.
    @Query("""
            select t from Transaction t
            join fetch t.account
            left join fetch t.category
            where t.id = :id and t.user.id = :userId
            """)
    Optional<Transaction> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query(value = """
            select t from Transaction t
            join fetch t.account
            left join fetch t.category
            where t.user.id = :userId
              and (:accountId is null or t.account.id = :accountId)
              and (:type is null or t.type = :type)
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
            order by t.occurredOn desc, t.createdAt desc
            """,
            countQuery = """
            select count(t) from Transaction t
            where t.user.id = :userId
              and (:accountId is null or t.account.id = :accountId)
              and (:type is null or t.type = :type)
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
            """)
    Page<Transaction> search(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable);

    @Query("""
            select t.type as type, sum(t.amount) as total from Transaction t
            where t.user.id = :userId
              and (:from is null or t.occurredOn >= :from)
              and (:to is null or t.occurredOn <= :to)
            group by t.type
            """)
    List<TypeTotal> summarize(
            @Param("userId") UUID userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("""
            select coalesce(sum(t.amount), 0) from Transaction t
            where t.account.id = :accountId
              and t.type = com.codewithsubhra.money_tracker_backend.transaction.domain.TransactionType.EXPENSE
              and t.occurredOn >= :from
              and t.occurredOn <= :to
            """)
    java.math.BigDecimal sumExpense(
            @Param("accountId") UUID accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Projection used for the income/expense summary. */
    interface TypeTotal {
        TransactionType getType();

        java.math.BigDecimal getTotal();
    }
}
