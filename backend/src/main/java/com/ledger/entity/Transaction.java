package com.ledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single, immutable ledger entry. Rows in this table are NEVER updated or
 * deleted after they reach COMPLETED status — only inserted. This gives the
 * system a full, replayable audit trail and lets balances be recomputed
 * deterministically at any point in time.
 *
 * sourceAccount / destinationAccount are nullable in combination based on
 * transactionType:
 *   DEPOSIT    -> source = null,        destination = account credited
 *   WITHDRAWAL -> source = account debited, destination = null
 *   TRANSFER   -> source = debited account, destination = credited account
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_source_account", columnList = "source_account_id"),
        @Index(name = "idx_tx_destination_account", columnList = "destination_account_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id")
    private Account destinationAccount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    /**
     * Optional idempotency/correlation key so retried client requests
     * (e.g. a mobile app resubmitting after a timeout) never create a
     * duplicate financial movement.
     */
    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @PrePersist
    void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
