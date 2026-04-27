package org.example.securitytokenservice.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {
    private final Properties properties;
    private final Properties environment;

    private AppConfig(Properties properties, Properties environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public static AppConfig load() {
        Properties properties = new Properties();
        Properties environment = new Properties();
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load application.properties", e);
        }
        System.getenv().forEach(environment::put);
        return new AppConfig(properties, environment);
    }

    private String getProperty(String key) {
        String envKey = key.replace(".", "_").toUpperCase();
        String envValue = (String) environment.get(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return properties.getProperty(key);
    }

    public String dbUrl() {
        return getProperty("app.db.url");
    }

    public String dbUser() {
        return getProperty("app.db.user");
    }

    public String dbPassword() {
        return getProperty("app.db.password");
    }

    public int port() {
        return Integer.parseInt(getProperty("app.port"));
    }

    public String tokenSecret() {
        return getProperty("app.token.secret");
    }

    public long tokenTtlMinutes() {
        return Long.parseLong(getProperty("app.token.ttl.minutes"));
    }

    public int defaultOtpLength() {
        return Integer.parseInt(getProperty("app.otp.default.length"));
    }

    public long defaultOtpTtlSeconds() {
        return Long.parseLong(getProperty("app.otp.default.ttl.seconds"));
    }

    public long schedulerDelaySeconds() {
        return Long.parseLong(getProperty("app.otp.scheduler.fixed.delay.seconds"));
    }

    public String otpFilePath() {
        return getProperty("app.file.otp.path");
    }

    public String emailResource() {
        String value = getProperty("app.email.resource");
        return value == null || value.isBlank() ? "email.properties" : value;
    }

    public String smsResource() {
        String value = getProperty("app.sms.resource");
        return value == null || value.isBlank() ? "sms.properties" : value;
    }

    public String telegramResource() {
        String value = getProperty("app.telegram.resource");
        return value == null || value.isBlank() ? "telegram.properties" : value;
    }

    public Properties loadResourceProperties(String resourceName) {
        Properties resourceProps = new Properties();

        Path path = Path.of(resourceName);
        if (path.isAbsolute() || resourceName.contains("/") || resourceName.contains("\\")) {
            if (!Files.exists(path)) {
                return resourceProps;
            }
            try (InputStream inputStream = Files.newInputStream(path)) {
                resourceProps.load(inputStream);
                return resourceProps;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load resource properties: " + resourceName, e);
            }
        }

        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                return resourceProps;
            }
            resourceProps.load(inputStream);
            return resourceProps;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resource properties: " + resourceName, e);
        }
    }
}
