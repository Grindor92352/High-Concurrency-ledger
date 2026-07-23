package com.ledger.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        Long accountId,
        String accountNumber,
        BigDecimal balance,
        Instant asOf
) {
}
