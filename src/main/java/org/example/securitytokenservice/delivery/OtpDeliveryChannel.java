package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.model.OtpCode;

public interface OtpDeliveryChannel {
    void send(OtpCode otpCode, String code);

    default void close() {
    }
}
