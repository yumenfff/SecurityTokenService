package org.example.securitytokenservice.dao;

import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.model.Role;
import org.example.securitytokenservice.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    private final Database database;

    public UserDao(Database database) {
        this.database = database;
    }

    public Optional<User> findByLogin(String login) {
        try (Connection connection = database.open()) {
            return findByLogin(connection, login);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load user by login", e);
        }
    }

    public Optional<User> findById(long id) {
        try (Connection connection = database.open()) {
            return findById(connection, id);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load user by id", e);
        }
    }

    public boolean existsAdmin(Connection connection) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next();
        }
    }

    public Optional<User> findByLogin(Connection connection, String login) throws SQLException {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE login = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public Optional<User> findById(Connection connection, long id) throws SQLException {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapUser(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public User create(String login, String passwordHash, Role role) {
        try (Connection connection = database.open()) {
            return create(connection, login, passwordHash, role);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create user", e);
        }
    }

    public User create(Connection connection, String login, String passwordHash, Role role) throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, login);
            statement.setString(2, passwordHash);
            statement.setString(3, role.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException("Failed to read generated user id");
                }
                long id = keys.getLong(1);
                return findById(connection, id).orElseThrow(() -> new IllegalStateException("Created user not found"));
            }
        }
    }

    public List<User> listNonAdmins() {
        try (Connection connection = database.open()) {
            return listNonAdmins(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load users", e);
        }
    }

    public List<User> listNonAdmins(Connection connection) throws SQLException {
        String sql = "SELECT id, login, password_hash, role, created_at FROM users WHERE role <> 'ADMIN' ORDER BY id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(mapUser(resultSet));
            }
        }
        return users;
    }

    public void deleteById(long id) {
        try (Connection connection = database.open()) {
            deleteById(connection, id);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user", e);
        }
    }

    public void deleteById(Connection connection, long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private static User mapUser(ResultSet resultSet) throws SQLException {
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        return new User(
                resultSet.getLong("id"),
                resultSet.getString("login"),
                resultSet.getString("password_hash"),
                Role.valueOf(resultSet.getString("role")),
                createdAt == null ? null : createdAt.toInstant()
        );
    }
}
