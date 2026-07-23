package com.ledger.repository;

import com.ledger.entity.Transaction;
import com.ledger.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Finds all completed transactions for an account (both debits and credits) to compute balance.
     * Uses JOIN FETCH to eagerly load account data and prevent lazy loading issues outside transaction scope.
     */
    @Query("""
           SELECT t FROM Transaction t
           LEFT JOIN FETCH t.sourceAccount
           LEFT JOIN FETCH t.destinationAccount
           WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
           AND t.status = :status
           ORDER BY t.timestamp ASC
           """)
    List<Transaction> findLedgerEntriesForAccount(@Param("accountId") Long accountId,
                                                   @Param("status") TransactionStatus status);

    /**
     * Paginated transaction query for an account's transaction history view.
     */
    @Query(value = """
           SELECT t FROM Transaction t
           LEFT JOIN FETCH t.sourceAccount
           LEFT JOIN FETCH t.destinationAccount
           WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
           AND t.status = :status
           """,
           countQuery = """
           SELECT COUNT(t) FROM Transaction t
           WHERE (t.sourceAccount.id = :accountId OR t.destinationAccount.id = :accountId)
           AND t.status = :status
           """)
    Page<Transaction> findLedgerEntriesForAccountPaged(@Param("accountId") Long accountId,
                                                        @Param("status") TransactionStatus status,
                                                        Pageable pageable);

    /**
     * Looks up an existing transaction by idempotency key to prevent duplicate payments.
     */
    @EntityGraph(attributePaths = {"sourceAccount", "destinationAccount"})
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}
