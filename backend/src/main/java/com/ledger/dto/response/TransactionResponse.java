package com.ledger.dto.response;

import com.ledger.entity.TransactionStatus;
import com.ledger.entity.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        Instant timestamp,
        TransactionType transactionType,
        TransactionStatus status
) {
}
