package com.ledger.entity;

/**
 * Application-level roles. Stored as STRING (not ORDINAL) in the database
 * so that reordering this enum never silently corrupts persisted data.
 */
public enum Role {
    ADMIN,
    CUSTOMER
}
