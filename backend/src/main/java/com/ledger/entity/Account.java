package com.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * IMPORTANT — IMMUTABLE LEDGER CONSTRAINT:
 * This entity intentionally has NO `balance` column. A mutable balance
 * column invites race conditions (read-modify-write) and destroys the
 * audit trail. The balance is always a derived value, computed on demand
 * by TransactionService#calculateBalance by folding over this account's
 * Transaction history. Account rows are pure identity/metadata; Transaction
 * rows are the source of truth for money.
 */
@Entity
@Table(name = "accounts", uniqueConstraints = @UniqueConstraint(columnNames = "account_number"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Optimistic version column. Even though money movement is protected by
     * PESSIMISTIC_WRITE row locks (see AccountRepository), this @Version
     * field gives Hibernate a cheap secondary guard: any accidental,
     * lock-bypassing update path (e.g. an admin edit screen) will fail
     * fast with an OptimisticLockException instead of silently overwriting
     * a concurrent change.
     */
    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
