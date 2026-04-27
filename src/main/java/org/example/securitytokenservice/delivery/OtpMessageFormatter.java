package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.model.OtpCode;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class OtpMessageFormatter {
    private static final DateTimeFormatter CREATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private OtpMessageFormatter() {
    }

    public static String format(OtpCode otpCode, String code) {
        long validSeconds = Math.max(0, Duration.between(otpCode.createdAt(), otpCode.expiresAt()).toSeconds());
        return String.format(
                "Ваш код: %s%nОперация: %s%nВалиден: %d сек%nДата создания: %s",
                code,
                otpCode.operationId(),
                validSeconds,
                CREATED_AT_FORMATTER.format(otpCode.createdAt())
        );
    }
}
