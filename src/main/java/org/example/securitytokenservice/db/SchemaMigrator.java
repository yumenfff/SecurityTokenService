package org.example.securitytokenservice.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

public final class SchemaMigrator {
    private static final Logger log = LoggerFactory.getLogger(SchemaMigrator.class);

    private SchemaMigrator() {
    }

    public static void migrate(Database database) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(true);
            String sql = readResource("schema.sql");
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement jdbcStatement = connection.createStatement()) {
                        jdbcStatement.execute(trimmed);
                    }
                }
            }
            log.info("Database schema checked successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    private static String readResource(String resourceName) throws IOException {
        try (InputStream inputStream = SchemaMigrator.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Resource not found: " + resourceName);
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            }
            return builder.toString();
        }
    }
}
