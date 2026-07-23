package com.ledger.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Used for both deposit and withdrawal endpoints — single account, single amount. */
public record AmountRequest(
        @NotNull(message = "accountId is required")
        Long accountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be positive")
        BigDecimal amount,

        String idempotencyKey
) {
}
