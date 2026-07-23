package com.ledger.service;

import com.ledger.entity.RefreshToken;
import com.ledger.entity.User;
import com.ledger.exception.TokenRefreshException;
import com.ledger.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${ledger.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Validates a presented refresh token and, if valid, ROTATES it:
     * the old token is revoked and a brand-new one is issued. Rotation
     * means a stolen-and-replayed refresh token is only useful once — the
     * legitimate client's next refresh attempt with the (now-revoked) old
     * token will fail loudly, which is a strong signal of token theft.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String presentedToken) {
        RefreshToken existing = refreshTokenRepository.findByToken(presentedToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found."));

        if (existing.isRevoked()) {
            throw new TokenRefreshException("Refresh token has been revoked. Please log in again.");
        }
        if (existing.getExpiryDate().isBefore(Instant.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createRefreshToken(existing.getUser());
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllForUser(user);
    }
}
