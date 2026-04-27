package org.example.securitytokenservice.service;

import org.example.securitytokenservice.dao.OtpCodeDao;
import org.example.securitytokenservice.dao.OtpConfigDao;
import org.example.securitytokenservice.dao.UserDao;
import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.delivery.DeliveryChannelFactory;
import org.example.securitytokenservice.delivery.OtpDeliveryChannel;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.DeliveryChannelType;
import org.example.securitytokenservice.model.OtpCode;
import org.example.securitytokenservice.model.OtpConfig;
import org.example.securitytokenservice.model.OtpStatus;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.security.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private final Database database;
    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;
    private final UserDao userDao;
    private final DeliveryChannelFactory deliveryChannelFactory;
    private final OtpCodeGenerator otpCodeGenerator;
    private final Clock clock;

    public OtpService(Database database,
                      OtpConfigDao otpConfigDao,
                      OtpCodeDao otpCodeDao,
                      UserDao userDao,
                      DeliveryChannelFactory deliveryChannelFactory,
                      OtpCodeGenerator otpCodeGenerator) {
        this(database, otpConfigDao, otpCodeDao, userDao, deliveryChannelFactory, otpCodeGenerator, Clock.systemUTC());
    }

    public OtpService(Database database,
                      OtpConfigDao otpConfigDao,
                      OtpCodeDao otpCodeDao,
                      UserDao userDao,
                      DeliveryChannelFactory deliveryChannelFactory,
                      OtpCodeGenerator otpCodeGenerator,
                      Clock clock) {
        this.database = database;
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = otpCodeDao;
        this.userDao = userDao;
        this.deliveryChannelFactory = deliveryChannelFactory;
        this.otpCodeGenerator = otpCodeGenerator;
        this.clock = clock;
    }

    public GeneratedOtp generate(User user, String operationId, String destination, DeliveryChannelType channelType) {
        if (operationId == null || operationId.isBlank()) {
            throw new ApiException(400, "Operation id must not be empty");
        }
        if (destination == null || destination.isBlank()) {
            throw new ApiException(400, "Destination must not be empty");
        }

        String normalizedOperationId = operationId.trim();
        String normalizedDestination = destination.trim();

        try (Connection connection = database.open()) {
            User existingUser = requireExistingUser(connection, user.id());
            OtpConfig config = otpConfigDao.getCurrent(connection);
            String code = otpCodeGenerator.generate(config.codeLength());
            Instant now = clock.instant();
            Instant expiresAt = now.plusSeconds(config.ttlSeconds());
            long otpId = otpCodeDao.insert(
                    connection,
                    existingUser.id(),
                    normalizedOperationId,
                    PasswordService.hash(code),
                    OtpStatus.ACTIVE,
                    channelType,
                    normalizedDestination,
                    expiresAt
            );

            OtpCode otpCode = new OtpCode(
                    otpId,
                    existingUser.id(),
                    normalizedOperationId,
                    null,
                    OtpStatus.ACTIVE,
                    channelType,
                    normalizedDestination,
                    now,
                    expiresAt,
                    null
            );
            OtpDeliveryChannel deliveryChannel = deliveryChannelFactory.get(channelType);
            deliveryChannel.send(otpCode, code);

            log.info("OTP generated for user {} operation {} via {}", existingUser.login(), normalizedOperationId, channelType);
            return new GeneratedOtp(otpId, expiresAt, channelType);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save OTP code", e);
        }
    }

    public boolean validate(User user, String operationId, String code) {
        if (operationId == null || operationId.isBlank()) {
            throw new ApiException(400, "Operation id must not be empty");
        }
        if (code == null || code.isBlank()) {
            throw new ApiException(400, "OTP code must not be empty");
        }

        try (Connection connection = database.open()) {
            User existingUser = requireExistingUser(connection, user.id());
            OtpCode otpCode = otpCodeDao.findLatestActiveByOperationAndUser(connection, existingUser.id(), operationId.trim())
                    .orElseThrow(() -> new ApiException(400, "Active OTP code not found"));

            Instant now = clock.instant();
            if (otpCode.expiresAt().isBefore(now)) {
                otpCodeDao.markExpired(connection, otpCode.id());
                throw new ApiException(400, "OTP code expired");
            }

            if (!PasswordService.matches(code.trim(), otpCode.codeHash())) {
                throw new ApiException(400, "Invalid OTP code");
            }

            otpCodeDao.markUsed(connection, otpCode.id(), now);
            log.info("OTP validated for user {} operation {}", existingUser.login(), operationId);
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to validate OTP code", e);
        }
    }

    private User requireExistingUser(Connection connection, long userId) throws SQLException {
        return userDao.findById(connection, userId)
                .orElseThrow(() -> new ApiException(401, "Invalid or expired token"));
    }

    public int expireOutdated() {
        try (Connection connection = database.open()) {
            return otpCodeDao.expireOutdated(connection, clock.instant());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to expire OTP codes", e);
        }
    }

    public record GeneratedOtp(long otpId, Instant expiresAt, DeliveryChannelType channelType) {
    }
}
