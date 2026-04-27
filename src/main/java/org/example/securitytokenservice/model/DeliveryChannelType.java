package org.example.securitytokenservice.model;

import org.example.securitytokenservice.error.ApiException;

public enum DeliveryChannelType {
    EMAIL,
    SMS,
    FILE,
    TELEGRAM;

    public static DeliveryChannelType from(String value) {
        if (value == null || value.isBlank()) {
            return FILE;
        }

        try {
            return DeliveryChannelType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(400, "Unknown delivery channel: " + value);
        }
    }
}
