package com.ledger.entity;

/**
 * Lifecycle state of a ledger entry.
 *
 * Only COMPLETED transactions are ever counted when calculating a
 * balance (see TransactionService#calculateBalance). This means a FAILED
 * or PENDING row can safely exist in the table (e.g. for audit trails on
 * a rejected transfer) without ever affecting money math.
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
