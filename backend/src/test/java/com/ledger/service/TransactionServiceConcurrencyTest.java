package com.ledger.service;

import com.ledger.entity.Account;
import com.ledger.entity.Role;
import com.ledger.entity.User;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency test using Testcontainers (PostgreSQL).
 * Fires multiple concurrent withdrawal requests against an account with balance for only one.
 * Verifies pessimistic locking prevents double spending and overdrawing.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@EnabledIf("isDockerAvailable")
class TransactionServiceConcurrencyTest {

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .username("concurrency-test-user-" + System.nanoTime())
                .passwordHash("irrelevant-for-this-test")
                .role(Role.CUSTOMER)
                .build());

        Account account = accountRepository.save(Account.builder()
                .accountNumber("TEST-" + System.nanoTime())
                .user(owner)
                .build());
        accountId = account.getId();

        // Fund the account with exactly enough for ONE withdrawal of 100.
        transactionService.deposit(accountId, new BigDecimal("100.00"));
    }

    @Test
    void concurrentWithdrawals_onlyOneShouldSucceed_dueToPessimisticLocking() throws InterruptedException {
        int attemptCount = 10;
        BigDecimal withdrawalAmount = new BigDecimal("100.00"); // exactly the full balance

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(attemptCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < attemptCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // all threads race to start at the same moment
                    transactionService.withdraw(accountId, withdrawalAmount);
                    successCount.incrementAndGet();
                } catch (Exception ex) {
                    rejectedCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads simultaneously
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get())
                .as("Exactly one withdrawal should succeed against a balance that only covers one")
                .isEqualTo(1);
        assertThat(rejectedCount.get()).isEqualTo(attemptCount - 1);

        // The definitive check: the final calculated balance must be exactly
        // zero, never negative. A broken (unlocked) implementation would let
        // multiple withdrawals through and drive this negative.
        BigDecimal finalBalance = transactionService.calculateBalance(accountId);
        assertThat(finalBalance).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
