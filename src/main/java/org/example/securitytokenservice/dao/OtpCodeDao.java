package org.example.securitytokenservice.dao;

import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.model.DeliveryChannelType;
import org.example.securitytokenservice.model.OtpCode;
import org.example.securitytokenservice.model.OtpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

public class OtpCodeDao {
    private final Database database;

    public OtpCodeDao(Database database) {
        this.database = database;
    }

    public long insert(Connection connection,
                      long userId,
                      String operationId,
                      String codeHash,
                      OtpStatus status,
                      DeliveryChannelType deliveryChannel,
                      String destination,
                      Instant expiresAt) throws SQLException {
        String sql = "INSERT INTO otp_codes (user_id, operation_id, code_hash, status, delivery_channel, destination, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, userId);
            statement.setString(2, operationId);
            statement.setString(3, codeHash);
            statement.setString(4, status.name());
            statement.setString(5, deliveryChannel.name());
            statement.setString(6, destination);
            statement.setTimestamp(7, Timestamp.from(expiresAt));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Failed to read generated OTP id");
                }
                return keys.getLong(1);
            }
        }
    }

    public Optional<OtpCode> findLatestActiveByOperationAndUser(Connection connection, long userId, String operationId) throws SQLException {
        String sql = "SELECT id, user_id, operation_id, code_hash, status, delivery_channel, destination, created_at, expires_at, used_at " +
                "FROM otp_codes WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE' ORDER BY created_at DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, operationId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCode(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public boolean markUsed(Connection connection, long id, Instant usedAt) throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'USED', used_at = ? WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(usedAt));
            statement.setLong(2, id);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean markExpired(Connection connection, long id) throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    public int expireOutdated(Connection connection, Instant now) throws SQLException {
        String sql = "UPDATE otp_codes SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND expires_at < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.from(now));
            return statement.executeUpdate();
        }
    }

    public int deleteByUserId(Connection connection, long userId) throws SQLException {
        String sql = "DELETE FROM otp_codes WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            return statement.executeUpdate();
        }
    }

    private static OtpCode mapCode(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        Timestamp expiresAt = resultSet.getTimestamp("expires_at");
        Timestamp usedAt = resultSet.getTimestamp("used_at");
        return new OtpCode(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("operation_id"),
                resultSet.getString("code_hash"),
                OtpStatus.valueOf(resultSet.getString("status")),
                DeliveryChannelType.valueOf(resultSet.getString("delivery_channel")),
                resultSet.getString("destination"),
                createdAt == null ? null : createdAt.toInstant(),
                expiresAt == null ? null : expiresAt.toInstant(),
                usedAt == null ? null : usedAt.toInstant()
        );
    }
}
