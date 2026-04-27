package org.example.securitytokenservice.service;

import java.security.SecureRandom;

public class OtpCodeGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("OTP length must be between 4 and 10");
        }

        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }
}
