package com.ledger.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 64, message = "username must be between 3 and 64 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {
    // Note: there is deliberately no `role` field here. Every self-registration
    // is forced to CUSTOMER in AuthService — clients can never grant themselves
    // ADMIN by tampering with the request body.
}
