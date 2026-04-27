package org.example.securitytokenservice.model;

import java.time.Instant;

public record OtpConfig(
        int id,
        long ttlSeconds,
        int codeLength,
        Instant updatedAt
) {
}
