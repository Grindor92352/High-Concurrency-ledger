package com.ledger.service;

import com.ledger.entity.Account;
import com.ledger.entity.User;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public Account createAccountFor(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .user(owner)
                .build();

        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account getAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional(readOnly = true)
    public List<Account> getAccountsForUser(String username) {
        return accountRepository.findByUser_Username(username);
    }

    /**
     * Admin view to get all registered accounts.
     */
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Checks if the user owns the account, or if the user is an admin.
     */
    public void verifyOwnershipOrAdmin(Account account, String requestingUsername, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!account.getUser().getUsername().equals(requestingUsername)) {
            throw new AccessDeniedException("You do not have access to account " + account.getId());
        }
    }

    /**
     * Generates a random unique account number string.
     */
    private String generateAccountNumber() {
        long suffix = Math.abs(RANDOM.nextLong() % 1_000_000_0000L);
        return "LEDGER%010d".formatted(suffix);
    }
}
