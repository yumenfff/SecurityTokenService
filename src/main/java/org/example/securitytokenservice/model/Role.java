package org.example.securitytokenservice.model;

import org.example.securitytokenservice.error.ApiException;

public enum Role {
    ADMIN,
    USER;

    public static Role from(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }

        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "Unknown role: " + value);
        }
    }
}
