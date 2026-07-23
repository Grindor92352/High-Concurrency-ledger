package com.ledger.service;

import com.ledger.dto.request.LoginRequest;
import com.ledger.dto.request.RegisterRequest;
import com.ledger.dto.response.AuthResponse;
import com.ledger.entity.RefreshToken;
import com.ledger.entity.Role;
import com.ledger.entity.User;
import com.ledger.repository.UserRepository;
import com.ledger.security.JwtService;
import com.ledger.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").passwordHash("hashed").role(Role.CUSTOMER).build();
        refreshToken = RefreshToken.builder().id(1L).user(user).token("ref-123").build();
    }

    @Test
    void register_savesCustomerAndReturnsAuthResponse() {
        RegisterRequest req = new RegisterRequest("alice", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(refreshToken);

        AuthResponse resp = authService.register(req);

        assertThat(resp).isNotNull();
        assertThat(resp.accessToken()).isEqualTo("jwt-token");
        assertThat(resp.refreshToken()).isEqualTo("ref-123");
    }

    @Test
    void register_throwsConflict_whenUsernameTaken() {
        RegisterRequest req = new RegisterRequest("alice", "password123");
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void login_authenticatesAndReturnsTokens() {
        LoginRequest req = new LoginRequest("alice", "password123");
        UserPrincipal principal = new UserPrincipal(user);
        var authResult = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authResult);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse resp = authService.login(req);

        assertThat(resp).isNotNull();
        assertThat(resp.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void logout_delegatesToRefreshTokenService() {
        authService.logout("ref-123");

        verify(refreshTokenService).revokeToken("ref-123");
    }
}
