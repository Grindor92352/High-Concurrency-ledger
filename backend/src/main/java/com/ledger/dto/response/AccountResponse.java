package com.ledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerUsername,
        Instant createdAt,
        BigDecimal balance
) {
}
