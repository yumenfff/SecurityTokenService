package org.example.securitytokenservice.service;

import org.example.securitytokenservice.config.AppConfig;
import org.example.securitytokenservice.dao.UserDao;
import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.Role;
import org.example.securitytokenservice.model.User;
import org.example.securitytokenservice.security.PasswordService;
import org.example.securitytokenservice.security.TokenClaims;
import org.example.securitytokenservice.security.TokenService;

import java.sql.Connection;
import java.sql.SQLException;

public class AuthService {
    private final Database database;
    private final UserDao userDao;
    private final TokenService tokenService;

    public AuthService(Database database, UserDao userDao, AppConfig config) {
        this.database = database;
        this.userDao = userDao;
        this.tokenService = new TokenService(config.tokenSecret(), config.tokenTtlMinutes());
    }

    public User register(String login, String password, String roleValue) {
        validateCredentials(login, password);
        Role role = Role.from(roleValue);

        try (Connection connection = database.open()) {
            if (role == Role.ADMIN && userDao.existsAdmin(connection)) {
                throw new ApiException(409, "Admin already exists");
            }

            String passwordHash = PasswordService.hash(password);
            return userDao.create(connection, login.trim(), passwordHash, role);
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new ApiException(409, "User with this login already exists");
            }
            throw new IllegalStateException("Failed to register user", e);
        }
    }

    public TokenService.IssuedToken login(String login, String password) {
        validateCredentials(login, password);
        User user = userDao.findByLogin(login.trim())
                .orElseThrow(() -> new ApiException(401, "Invalid login or password"));

        if (!PasswordService.matches(password, user.passwordHash())) {
            throw new ApiException(401, "Invalid login or password");
        }

        return tokenService.issueToken(user);
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    public User requireExistingUser(TokenClaims claims) {
        return userDao.findById(claims.userId())
                .orElseThrow(() -> new ApiException(401, "Invalid or expired token"));
    }

    private static void validateCredentials(String login, String password) {
        if (login == null || login.isBlank()) {
            throw new ApiException(400, "Login must not be empty");
        }
        if (login.indexOf(':') >= 0) {
            throw new ApiException(400, "Login must not contain ':'");
        }
        if (password == null || password.isBlank()) {
            throw new ApiException(400, "Password must not be empty");
        }
    }

    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
