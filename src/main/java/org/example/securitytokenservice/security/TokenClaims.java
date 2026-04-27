package org.example.securitytokenservice.security;

import org.example.securitytokenservice.model.Role;

import java.time.Instant;

public record TokenClaims(
        long userId,
        String login,
        Role role,
        Instant issuedAt,
        Instant expiresAt
) {
}
