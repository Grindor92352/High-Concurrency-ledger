package com.ledger.service;

import com.ledger.dto.request.CreateUserRequest;
import com.ledger.dto.request.LoginRequest;
import com.ledger.dto.request.RegisterRequest;
import com.ledger.dto.response.AuthResponse;
import com.ledger.dto.response.UserResponse;
import com.ledger.entity.RefreshToken;
import com.ledger.entity.Role;
import com.ledger.entity.User;
import com.ledger.repository.UserRepository;
import com.ledger.security.JwtService;
import com.ledger.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${ledger.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Registers a new customer user with BCrypt hashed password.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        userRepository.save(user);

        return issueTokens(user);
    }

    /**
     * Authenticates username & password using Spring Security AuthenticationManager.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return issueTokens(user);
    }

    /**
     * Rotates refresh token and issues a new JWT access token.
     */
    @Transactional
    public AuthResponse refresh(String presentedRefreshToken) {
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(presentedRefreshToken);
        User user = rotated.getUser();

        String newAccessToken = jwtService.generateToken(new UserPrincipal(user));
        return AuthResponse.of(newAccessToken, rotated.getToken(), expirationMs);
    }

    /**
     * Revokes the refresh token on user logout.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeToken(refreshToken);
    }

    /**
     * Allows admin to create users with specific roles (CUSTOMER or ADMIN).
     */
    @Transactional
    public UserResponse adminCreateUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }

        User user = User.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRole());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateToken(new UserPrincipal(user));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        return AuthResponse.of(accessToken, refreshToken.getToken(), expirationMs);
    }
}
