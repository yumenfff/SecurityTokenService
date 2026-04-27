package org.example.securitytokenservice.model;

import java.time.Instant;

public record OtpCode(
        long id,
        long userId,
        String operationId,
        String codeHash,
        OtpStatus status,
        DeliveryChannelType deliveryChannel,
        String destination,
        Instant createdAt,
        Instant expiresAt,
        Instant usedAt
) {
}
