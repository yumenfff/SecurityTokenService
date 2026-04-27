package org.example.securitytokenservice.delivery;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.model.OtpCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class FileOtpDeliveryChannel implements OtpDeliveryChannel {
    private static final Logger log = LoggerFactory.getLogger(FileOtpDeliveryChannel.class);
    private static final String DEFAULT_FILENAME = "otp-codes.txt";
    private final Path filePath;

    public FileOtpDeliveryChannel(AppConfig config) {
        this.filePath = Path.of(config.otpFilePath());
    }

    @Override
    public void send(OtpCode otpCode, String code) {
        String line = OtpMessageFormatter.format(otpCode, code)
                + System.lineSeparator()
                + "--------------------------------------------------"
                + System.lineSeparator();

        try {
            Path targetFile = resolveTargetFile(filePath);
            Path parent = targetFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(targetFile, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("OTP saved to file {}", targetFile.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save OTP to file", e);
        }
    }

    private static Path resolveTargetFile(Path configuredPath) throws IOException {
        if (Files.exists(configuredPath) && Files.isDirectory(configuredPath)) {
            Path fallback = configuredPath.resolve(DEFAULT_FILENAME);
            log.warn("Configured OTP path {} is a directory. Fallback file: {}",
                    configuredPath.toAbsolutePath(),
                    fallback.toAbsolutePath());
            return fallback;
        }
        return configuredPath;
    }
}
