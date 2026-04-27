package org.example.securitytokenservice.db;

import org.example.securitytokenservice.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final AppConfig config;

    public Database(AppConfig config) {
        this.config = config;
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    public AppConfig config() {
        return config;
    }
}
