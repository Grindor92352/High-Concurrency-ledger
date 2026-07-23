package com.ledger.service;

import com.ledger.entity.Account;
import com.ledger.entity.Transaction;
import com.ledger.entity.TransactionStatus;
import com.ledger.entity.TransactionType;
import com.ledger.entity.User;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.exception.InsufficientFundsException;
import com.ledger.exception.InvalidTransactionException;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account acc1;
    private Account acc2;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).username("testuser").build();
        acc1 = Account.builder().id(100L).accountNumber("ACC-100").user(user).build();
        acc2 = Account.builder().id(200L).accountNumber("ACC-200").user(user).build();
    }

    @Test
    void calculateBalance_sumsCreditsAndDebitsCorrectly() {
        when(accountRepository.existsById(100L)).thenReturn(true);

        Transaction deposit = Transaction.builder()
                .id(1L).destinationAccount(acc1).amount(new BigDecimal("500.00"))
                .status(TransactionStatus.COMPLETED).transactionType(TransactionType.DEPOSIT).build();

        Transaction withdrawal = Transaction.builder()
                .id(2L).sourceAccount(acc1).amount(new BigDecimal("150.00"))
                .status(TransactionStatus.COMPLETED).transactionType(TransactionType.WITHDRAWAL).build();

        Transaction transferOut = Transaction.builder()
                .id(3L).sourceAccount(acc1).destinationAccount(acc2).amount(new BigDecimal("50.00"))
                .status(TransactionStatus.COMPLETED).transactionType(TransactionType.TRANSFER).build();

        when(transactionRepository.findLedgerEntriesForAccount(100L, TransactionStatus.COMPLETED))
                .thenReturn(List.of(deposit, withdrawal, transferOut));

        BigDecimal balance = transactionService.calculateBalance(100L);

        assertThat(balance).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void calculateBalance_throwsAccountNotFoundException_whenAccountDoesNotExist() {
        when(accountRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> transactionService.calculateBalance(999L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void deposit_createsAndSavesDepositRecord() {
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(acc1));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            return Transaction.builder()
                    .id(10L)
                    .destinationAccount(tx.getDestinationAccount())
                    .amount(tx.getAmount())
                    .timestamp(Instant.now())
                    .transactionType(tx.getTransactionType())
                    .status(tx.getStatus())
                    .build();
        });

        Transaction tx = transactionService.deposit(100L, new BigDecimal("250.00"));

        assertThat(tx).isNotNull();
        assertThat(tx.getId()).isEqualTo(10L);
        assertThat(tx.getDestinationAccount()).isEqualTo(acc1);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void deposit_throwsInvalidTransaction_whenAmountIsZeroOrNegative() {
        assertThatThrownBy(() -> transactionService.deposit(100L, BigDecimal.ZERO))
                .isInstanceOf(InvalidTransactionException.class);
        assertThatThrownBy(() -> transactionService.deposit(100L, new BigDecimal("-10.00")))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void withdraw_throwsInsufficientFunds_whenBalanceIsTooLow() {
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(acc1));
        when(accountRepository.existsById(100L)).thenReturn(true);
        when(transactionRepository.findLedgerEntriesForAccount(100L, TransactionStatus.COMPLETED))
                .thenReturn(List.of()); // 0 balance

        assertThatThrownBy(() -> transactionService.withdraw(100L, new BigDecimal("100.00")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void transferFunds_throwsInvalidTransaction_whenTransferringToSameAccount() {
        assertThatThrownBy(() -> transactionService.transferFunds(100L, 100L, new BigDecimal("50.00")))
                .isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void transferFunds_replaysExistingTransaction_whenIdempotencyKeyMatches() {
        Transaction existing = Transaction.builder()
                .id(99L)
                .sourceAccount(acc1)
                .destinationAccount(acc2)
                .amount(new BigDecimal("50.00"))
                .idempotencyKey("KEY-123")
                .build();

        when(transactionRepository.findByIdempotencyKey("KEY-123")).thenReturn(Optional.of(existing));

        Transaction result = transactionService.transferFunds(100L, 200L, new BigDecimal("50.00"), "KEY-123");

        assertThat(result.getId()).isEqualTo(99L);
        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void transferFunds_successfulTransfer() {
        when(accountRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(acc1));
        when(accountRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(acc2));
        when(accountRepository.existsById(100L)).thenReturn(true);

        // Account 100 has 500 balance
        Transaction initialDeposit = Transaction.builder()
                .destinationAccount(acc1)
                .amount(new BigDecimal("500.00"))
                .status(TransactionStatus.COMPLETED)
                .build();
        when(transactionRepository.findLedgerEntriesForAccount(100L, TransactionStatus.COMPLETED))
                .thenReturn(List.of(initialDeposit));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            return Transaction.builder()
                    .id(55L)
                    .sourceAccount(tx.getSourceAccount())
                    .destinationAccount(tx.getDestinationAccount())
                    .amount(tx.getAmount())
                    .status(tx.getStatus())
                    .transactionType(tx.getTransactionType())
                    .build();
        });

        Transaction tx = transactionService.transferFunds(100L, 200L, new BigDecimal("200.00"));

        assertThat(tx).isNotNull();
        assertThat(tx.getId()).isEqualTo(55L);
        assertThat(tx.getSourceAccount().getId()).isEqualTo(100L);
        assertThat(tx.getDestinationAccount().getId()).isEqualTo(200L);
        assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }
}
