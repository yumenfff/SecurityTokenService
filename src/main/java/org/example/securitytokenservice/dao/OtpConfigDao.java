package org.example.securitytokenservice.dao;

import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.model.OtpConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class OtpConfigDao {
    private final Database database;

    public OtpConfigDao(Database database) {
        this.database = database;
    }

    public OtpConfig getCurrent() {
        try (Connection connection = database.open()) {
            return getCurrent(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load OTP config", e);
        }
    }

    public OtpConfig getCurrent(Connection connection) throws SQLException {
        String sql = "SELECT id, ttl_seconds, code_length, updated_at FROM otp_config ORDER BY updated_at DESC, id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                throw new IllegalStateException("OTP config row not found");
            }
            return mapConfig(resultSet);
        }
    }

    public OtpConfig update(long ttlSeconds, int codeLength) {
        try (Connection connection = database.open()) {
            return update(connection, ttlSeconds, codeLength);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update OTP config", e);
        }
    }

    public OtpConfig update(Connection connection, long ttlSeconds, int codeLength) throws SQLException {
        String sql = "UPDATE otp_config SET ttl_seconds = ?, code_length = ?, updated_at = NOW()";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ttlSeconds);
            statement.setInt(2, codeLength);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                try (PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO otp_config (id, ttl_seconds, code_length, updated_at) VALUES (1, ?, ?, NOW())")) {
                    insertStatement.setLong(1, ttlSeconds);
                    insertStatement.setInt(2, codeLength);
                    insertStatement.executeUpdate();
                }
            }
        }
        return getCurrent(connection);
    }

    private static OtpConfig mapConfig(ResultSet resultSet) throws SQLException {
        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        return new OtpConfig(
                resultSet.getInt("id"),
                resultSet.getLong("ttl_seconds"),
                resultSet.getInt("code_length"),
                updatedAt == null ? null : updatedAt.toInstant()
        );
    }
}
