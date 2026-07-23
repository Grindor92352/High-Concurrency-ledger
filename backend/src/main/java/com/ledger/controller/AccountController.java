package com.ledger.controller;

import com.ledger.dto.response.AccountResponse;
import com.ledger.dto.response.BalanceResponse;
import com.ledger.entity.Account;
import com.ledger.security.UserPrincipal;
import com.ledger.service.AccountService;
import com.ledger.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.createAccountFor(principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(account, BigDecimal.ZERO));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<AccountResponse>> myAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        List<AccountResponse> accounts = accountService.getAccountsForUser(principal.getUsername()).stream()
                .map(a -> toResponse(a, transactionService.calculateBalance(a.getId())))
                .toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getAccountOrThrow(accountId);
        accountService.verifyOwnershipOrAdmin(account, principal.getUsername(), isAdmin(principal));
        BigDecimal balance = transactionService.calculateBalance(accountId);
        return ResponseEntity.ok(toResponse(account, balance));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long accountId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        Account account = accountService.getAccountOrThrow(accountId);
        accountService.verifyOwnershipOrAdmin(account, principal.getUsername(), isAdmin(principal));
        BigDecimal balance = transactionService.calculateBalance(accountId);
        return ResponseEntity.ok(new BalanceResponse(account.getId(), account.getAccountNumber(), balance, Instant.now()));
    }

    private boolean isAdmin(UserPrincipal principal) {
        return principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private AccountResponse toResponse(Account account, BigDecimal balance) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getUser().getUsername(),
                account.getCreatedAt(),
                balance
        );
    }
}
