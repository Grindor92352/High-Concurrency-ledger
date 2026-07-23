package com.ledger.controller;

import com.ledger.dto.request.AmountRequest;
import com.ledger.dto.request.TransferRequest;
import com.ledger.dto.response.PagedResponse;
import com.ledger.dto.response.TransactionResponse;
import com.ledger.entity.Account;
import com.ledger.entity.Transaction;
import com.ledger.security.UserPrincipal;
import com.ledger.service.AccountService;
import com.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        // Ownership check: the authenticated user must own the source account
        // (or be an admin) — you cannot debit an account that isn't yours.
        Account source = accountService.getAccountOrThrow(request.sourceAccountId());
        accountService.verifyOwnershipOrAdmin(source, principal.getUsername(), isAdmin(principal));

        Transaction result = transactionService.transferFunds(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                request.idempotencyKey());

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody AmountRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getAccountOrThrow(request.accountId());
        accountService.verifyOwnershipOrAdmin(account, principal.getUsername(), isAdmin(principal));

        Transaction result = transactionService.deposit(request.accountId(), request.amount(), request.idempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody AmountRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getAccountOrThrow(request.accountId());
        accountService.verifyOwnershipOrAdmin(account, principal.getUsername(), isAdmin(principal));

        Transaction result = transactionService.withdraw(request.accountId(), request.amount(), request.idempotencyKey());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<PagedResponse<TransactionResponse>> history(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {

        Account account = accountService.getAccountOrThrow(accountId);
        accountService.verifyOwnershipOrAdmin(account, principal.getUsername(), isAdmin(principal));

        // Newest-first; capped page size to stop a client from requesting
        // an unreasonably large page (e.g. ?size=1000000).
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("timestamp").descending());
        Page<Transaction> historyPage = transactionService.getHistory(accountId, pageable);

        return ResponseEntity.ok(PagedResponse.from(historyPage.map(this::toResponse)));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private TransactionResponse toResponse(Transaction tx) {
        return new TransactionResponse(
                tx.getId(),
                tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null,
                tx.getDestinationAccount() != null ? tx.getDestinationAccount().getAccountNumber() : null,
                tx.getAmount(),
                tx.getTimestamp(),
                tx.getTransactionType(),
                tx.getStatus()
        );
    }
}
