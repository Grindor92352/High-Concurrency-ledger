package com.ledger.dto.request;

import com.ledger.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Used only by AdminController — the one place in the system where a
 * request body is allowed to specify a Role directly. RegisterRequest
 * (self-service signup) has no such field on purpose.
 */
public record CreateUserRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 64, message = "username must be between 3 and 64 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password,

        @NotNull(message = "role is required")
        Role role
) {
}
