package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramOtpDeliveryChannel implements OtpDeliveryChannel {
    private static final Logger log = LoggerFactory.getLogger(TelegramOtpDeliveryChannel.class);
    private final AppConfig config;

    public TelegramOtpDeliveryChannel(AppConfig config) {
        this.config = config;
    }

    @Override
    public void send(OtpCode otpCode, String code) {
        Properties properties = config.loadResourceProperties(config.telegramResource());
        String botToken = require(properties, "telegram.bot_token");
        String chatId = require(properties, "telegram.chat_id");

        String apiUrl = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
        String message = OtpMessageFormatter.format(otpCode, code);
        String url = String.format("%s?chat_id=%s&text=%s", apiUrl, chatId, urlEncode(message));

        sendTelegramRequest(url);
        log.info("OTP sent by Telegram to {}", otpCode.destination());
    }

    private void sendTelegramRequest(String url) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode != 200) {
                log.error("Telegram API error. Status code: {}. Response: {}", statusCode, response.body());
                throw new IllegalStateException("Telegram API returned status code: " + statusCode);
            }
        } catch (InterruptedException e) {
            log.error("Error sending Telegram message: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to send OTP by Telegram", e);
        } catch (IOException e) {
            log.error("Error sending Telegram message: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to send OTP by Telegram", e);
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing Telegram config property: " + key);
        }
        return value;
    }
}
