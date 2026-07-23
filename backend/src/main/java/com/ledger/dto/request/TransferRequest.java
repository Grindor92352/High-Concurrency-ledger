package com.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "sourceAccountId is required")
        Long sourceAccountId,

        @NotNull(message = "destinationAccountId is required")
        Long destinationAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        /**
         * Optional client-supplied idempotency key. If a client retries the
         * same request (e.g. after a network timeout) with the same key,
         * the service returns the original transaction instead of moving
         * money twice.
         */
        String idempotencyKey
) {
}
