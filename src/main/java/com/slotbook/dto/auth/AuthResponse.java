package com.slotbook.dto.auth;

import com.slotbook.domain.Role;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMinutes,
        Long userId,
        String name,
        String email,
        Role role
) {
}