package org.example.securitytokenservice.service;

import org.example.securitytokenservice.dao.OtpCodeDao;
import org.example.securitytokenservice.dao.OtpConfigDao;
import org.example.securitytokenservice.dao.UserDao;
import org.example.securitytokenservice.db.Database;
import org.example.securitytokenservice.error.ApiException;
import org.example.securitytokenservice.model.OtpConfig;
import org.example.securitytokenservice.model.Role;
import org.example.securitytokenservice.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class AdminService {
    private final Database database;
    private final UserDao userDao;
    private final OtpConfigDao otpConfigDao;
    private final OtpCodeDao otpCodeDao;

    public AdminService(Database database, UserDao userDao, OtpConfigDao otpConfigDao, OtpCodeDao otpCodeDao) {
        this.database = database;
        this.userDao = userDao;
        this.otpConfigDao = otpConfigDao;
        this.otpCodeDao = otpCodeDao;
    }

    public List<User> listNonAdminUsers() {
        return userDao.listNonAdmins();
    }

    public OtpConfig updateOtpConfig(long ttlSeconds, int codeLength) {
        if (ttlSeconds <= 0) {
            throw new ApiException(400, "TTL must be positive");
        }
        if (codeLength < 4 || codeLength > 10) {
            throw new ApiException(400, "Code length must be between 4 and 10");
        }
        return otpConfigDao.update(ttlSeconds, codeLength);
    }

    public void deleteUser(long userId) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            User user = userDao.findById(connection, userId)
                    .orElseThrow(() -> new ApiException(404, "User not found"));
            if (user.role() == Role.ADMIN) {
                throw new ApiException(400, "Administrators cannot be deleted");
            }

            otpCodeDao.deleteByUserId(connection, userId);
            userDao.deleteById(connection, userId);
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user", e);
        }
    }
}
