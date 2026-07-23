package com.ledger.service;

import com.ledger.entity.Account;
import com.ledger.entity.User;
import com.ledger.exception.AccountNotFoundException;
import com.ledger.repository.AccountRepository;
import com.ledger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;
    private Account account;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("john_doe").build();
        account = Account.builder().id(10L).accountNumber("LEDGER1234567890").user(user).build();
    }

    @Test
    void createAccountFor_savesAndReturnsNewAccount() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(user));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
            Account acc = i.getArgument(0);
            return Account.builder().id(10L).accountNumber(acc.getAccountNumber()).user(user).build();
        });

        Account created = accountService.createAccountFor("john_doe");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void getAccountOrThrow_returnsAccount_whenFound() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        Account result = accountService.getAccountOrThrow(10L);

        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void getAccountOrThrow_throwsAccountNotFoundException_whenNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountOrThrow(99L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void verifyOwnershipOrAdmin_allowsOwner() {
        accountService.verifyOwnershipOrAdmin(account, "john_doe", false);
    }

    @Test
    void verifyOwnershipOrAdmin_allowsAdmin() {
        accountService.verifyOwnershipOrAdmin(account, "other_user", true);
    }

    @Test
    void verifyOwnershipOrAdmin_throwsAccessDenied_forOtherUser() {
        assertThatThrownBy(() -> accountService.verifyOwnershipOrAdmin(account, "other_user", false))
                .isInstanceOf(AccessDeniedException.class);
    }
}
