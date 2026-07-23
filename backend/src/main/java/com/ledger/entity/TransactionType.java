package com.ledger.entity;

/**
 * Classifies the nature of a ledger entry.
 *
 * DEPOSIT     - external money in.  sourceAccount is null, destinationAccount is set.
 * WITHDRAWAL  - external money out. sourceAccount is set, destinationAccount is null.
 * TRANSFER    - internal movement.  Both sourceAccount and destinationAccount are set.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
