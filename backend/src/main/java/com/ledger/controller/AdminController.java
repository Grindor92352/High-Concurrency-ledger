package com.ledger.controller;

import com.ledger.dto.request.CreateUserRequest;
import com.ledger.dto.response.AccountResponse;
import com.ledger.dto.response.UserResponse;
import com.ledger.service.AccountService;
import com.ledger.service.AuthService;
import com.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin endpoints for user creation and viewing all accounts. Protected with @PreAuthorize("hasRole('ADMIN')").
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuthService authService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    /**
     * Endpoint for admins to create new users (CUSTOMER or ADMIN).
     */
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.adminCreateUser(request));
    }

    /**
     * Fetches all registered accounts and their calculated balances across the system.
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> allAccounts() {
        List<AccountResponse> accounts = accountService.getAllAccounts().stream()
                .map(a -> new AccountResponse(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getUser().getUsername(),
                        a.getCreatedAt(),
                        transactionService.calculateBalance(a.getId())))
                .toList();
        return ResponseEntity.ok(accounts);
    }
}
