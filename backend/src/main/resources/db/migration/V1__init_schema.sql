-- V1__init_schema.sql
-- Initial schema for the High-Concurrency Financial Ledger.
-- Note: accounts has NO balance column by design (see Account.java / TransactionService.java).

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL
);

CREATE TABLE accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(34) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);

CREATE TABLE transactions (
    id                      BIGSERIAL PRIMARY KEY,
    source_account_id       BIGINT REFERENCES accounts(id),
    destination_account_id  BIGINT REFERENCES accounts(id),
    amount                  NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    "timestamp"             TIMESTAMPTZ NOT NULL DEFAULT now(),
    transaction_type        VARCHAR(20) NOT NULL,
    status                  VARCHAR(20) NOT NULL,
    idempotency_key         VARCHAR(64) UNIQUE,

    CONSTRAINT chk_transaction_endpoints CHECK (
        source_account_id IS NOT NULL OR destination_account_id IS NOT NULL
    )
);

CREATE INDEX idx_tx_source_account ON transactions(source_account_id);
CREATE INDEX idx_tx_destination_account ON transactions(destination_account_id);
CREATE INDEX idx_tx_status ON transactions(status);
