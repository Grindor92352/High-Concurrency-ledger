package com.ledger.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInMs
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInMs);
    }
}
