package org.example.securitytokenservice.model;

import java.time.Instant;

public record User(
        long id,
        String login,
        String passwordHash,
        Role role,
        Instant createdAt
) {
}
