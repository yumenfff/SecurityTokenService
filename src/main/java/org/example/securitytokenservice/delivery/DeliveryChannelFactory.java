package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.model.DeliveryChannelType;

public class DeliveryChannelFactory {
    private final OtpDeliveryChannel fileChannel;
    private final OtpDeliveryChannel emailChannel;
    private final OtpDeliveryChannel smsChannel;
    private final OtpDeliveryChannel telegramChannel;

    public DeliveryChannelFactory(AppConfig config) {
        this.fileChannel = new FileOtpDeliveryChannel(config);
        this.emailChannel = new EmailOtpDeliveryChannel(config);
        this.smsChannel = new SmsOtpDeliveryChannel(config);
        this.telegramChannel = new TelegramOtpDeliveryChannel(config);
    }

    public OtpDeliveryChannel get(DeliveryChannelType type) {
        return switch (type) {
            case FILE -> fileChannel;
            case EMAIL -> emailChannel;
            case SMS -> smsChannel;
            case TELEGRAM -> telegramChannel;
        };
    }

    public void close() {
        fileChannel.close();
        emailChannel.close();
        smsChannel.close();
        telegramChannel.close();
    }
}
