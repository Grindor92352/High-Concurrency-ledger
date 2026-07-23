package com.ledger.service;

import com.ledger.entity.Account;
import com.ledger.entity.Transaction;
import com.ledger.entity.TransactionStatus;
import com.ledger.entity.TransactionType;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidTransactionException;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    // =========================================================================================
    // BALANCE CALCULATION — calculated from transaction history
    // =========================================================================================

    /**
     * Calculates the current balance of an account by summing up all completed transactions.
     * We don't store a static balance column in DB, but calculate it live from the ledger.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBalance(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        List<Transaction> ledgerEntries =
                transactionRepository.findLedgerEntriesForAccount(accountId, TransactionStatus.COMPLETED);

        return ledgerEntries.stream()
                .map(tx -> signedAmountFor(tx, accountId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Returns signed amount: positive if money came in (credit), negative if money went out (debit).
     */
    private BigDecimal signedAmountFor(Transaction tx, Long accountId) {
        boolean isCredit = tx.getDestinationAccount() != null
                && tx.getDestinationAccount().getId().equals(accountId);
        boolean isDebit = tx.getSourceAccount() != null
                && tx.getSourceAccount().getId().equals(accountId);

        if (isCredit) {
            return tx.getAmount();
        }
        if (isDebit) {
            return tx.getAmount().negate();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Fetches full transaction history for an account (completed transactions).
     */
    @Transactional(readOnly = true)
    public List<Transaction> getHistory(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transactionRepository.findLedgerEntriesForAccount(accountId, TransactionStatus.COMPLETED);
    }

    /**
     * Fetches paginated transaction history for the UI.
     */
    @Transactional(readOnly = true)
    public Page<Transaction> getHistory(Long accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transactionRepository.findLedgerEntriesForAccountPaged(accountId, TransactionStatus.COMPLETED, pageable);
    }

    // =========================================================================================
    // TRANSFER PROCESSING — pessimistic locks to handle concurrency safely
    // =========================================================================================

    /**
     * Transfers money between two accounts. Uses SELECT FOR UPDATE locks to prevent double spending.
     * Always locks lower account ID first to prevent deadlocks.
     */
    public Transaction transferFunds(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        return transferFunds(sourceAccountId, destinationAccountId, amount, null);
    }

    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Transaction transferFunds(Long sourceAccountId, Long destinationAccountId, BigDecimal amount,
                                      String idempotencyKey) {
        validateTransferRequest(sourceAccountId, destinationAccountId, amount);

        // Check if request was already processed (idempotency check)
        Optional<Transaction> existing = findExistingByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay detected for key={}, returning existing txId={}",
                    idempotencyKey, existing.get().getId());
            return existing.get();
        }

        // Lock lower account ID first to prevent deadlocks when concurrent transfers happen in reverse order
        Long firstLockId = Math.min(sourceAccountId, destinationAccountId);
        Long secondLockId = Math.max(sourceAccountId, destinationAccountId);

        Account firstLocked = lockAccountOrThrow(firstLockId);
        Account secondLocked = lockAccountOrThrow(secondLockId);

        Account sourceAccount = sourceAccountId.equals(firstLockId) ? firstLocked : secondLocked;
        Account destinationAccount = destinationAccountId.equals(firstLockId) ? firstLocked : secondLocked;

        // Check if source account has enough balance
        BigDecimal sourceBalance = calculateBalance(sourceAccount.getId());
        if (sourceBalance.compareTo(amount) < 0) {
            log.warn("Rejected transfer of {} from account {} — available balance {}",
                    amount, sourceAccount.getId(), sourceBalance);
            throw new InsufficientFundsException(
                    "Account %d has insufficient funds: balance=%s, requested=%s"
                            .formatted(sourceAccount.getId(), sourceBalance, amount));
        }

        Transaction transferRecord = Transaction.builder()
                .sourceAccount(sourceAccount)
                .destinationAccount(destinationAccount)
                .amount(amount)
                .timestamp(Instant.now())
                .transactionType(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        Transaction saved = transactionRepository.save(transferRecord);

        log.info("Transfer completed: {} moved from account {} to account {} (txId={})",
                amount, sourceAccount.getId(), destinationAccount.getId(), saved.getId());

        return saved;
    }

    /**
     * Deposit funds into an account.
     */
    public Transaction deposit(Long accountId, BigDecimal amount) {
        return deposit(accountId, amount, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Transaction deposit(Long accountId, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Deposit amount must be positive.");
        }

        Optional<Transaction> existing = findExistingByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Account account = lockAccountOrThrow(accountId);

        Transaction depositRecord = Transaction.builder()
                .sourceAccount(null)
                .destinationAccount(account)
                .amount(amount)
                .timestamp(Instant.now())
                .transactionType(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        return transactionRepository.save(depositRecord);
    }

    /**
     * Withdraw funds from an account after checking balance.
     */
    public Transaction withdraw(Long accountId, BigDecimal amount) {
        return withdraw(accountId, amount, null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Transaction withdraw(Long accountId, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be positive.");
        }

        Optional<Transaction> existing = findExistingByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        Account account = lockAccountOrThrow(accountId);

        BigDecimal balance = calculateBalance(accountId);
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Account %d has insufficient funds: balance=%s, requested=%s"
                            .formatted(accountId, balance, amount));
        }

        Transaction withdrawalRecord = Transaction.builder()
                .sourceAccount(account)
                .destinationAccount(null)
                .amount(amount)
                .timestamp(Instant.now())
                .transactionType(TransactionType.WITHDRAWAL)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        return transactionRepository.save(withdrawalRecord);
    }

    // =========================================================================================
    // Helpers
    // =========================================================================================

    private Optional<Transaction> findExistingByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKey(idempotencyKey);
    }

    private Account lockAccountOrThrow(Long accountId) {
        Optional<Account> locked = accountRepository.findByIdForUpdate(accountId);
        return locked.orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private void validateTransferRequest(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        if (sourceAccountId == null || destinationAccountId == null) {
            throw new InvalidTransactionException("Source and destination account ids are required.");
        }
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new InvalidTransactionException("Cannot transfer funds to the same account.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive.");
        }
    }
}
