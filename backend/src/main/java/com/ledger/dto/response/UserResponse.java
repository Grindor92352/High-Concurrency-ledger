package com.ledger.dto.response;

import com.ledger.entity.Role;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
}
