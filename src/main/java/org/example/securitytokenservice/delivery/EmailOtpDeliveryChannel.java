package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.model.OtpCode;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class EmailOtpDeliveryChannel implements OtpDeliveryChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailOtpDeliveryChannel.class);
    private final AppConfig config;

    public EmailOtpDeliveryChannel(AppConfig config) {
        this.config = config;
    }

    @Override
    public void send(OtpCode otpCode, String code) {
        Properties properties = config.loadResourceProperties(config.emailResource());
        String username = require(properties, "email.username");
        String password = require(properties, "email.password");
        String fromEmail = require(properties, "email.from");
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(otpCode.destination()));
            message.setSubject("Your OTP code");
            message.setText(OtpMessageFormatter.format(otpCode, code));
            Transport.send(message);
            log.info("OTP sent by email to {}", otpCode.destination());
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send OTP by email", e);
        }
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing email config property: " + key);
        }
        return value;
    }
}
