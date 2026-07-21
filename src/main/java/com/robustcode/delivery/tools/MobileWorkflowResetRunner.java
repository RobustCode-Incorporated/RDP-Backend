package com.robustcode.delivery.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class MobileWorkflowResetRunner {

    private MobileWorkflowResetRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected a single SQL file path argument.");
        }

        String jdbcUrl = requireJdbcUrl();
        String username = requireEnv("NEON_DB_USERNAME", "PGUSER");
        String password = requireEnv("NEON_DB_PASSWORD", "PGPASSWORD");
        String sql = readSqlFile(args[0]);

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }

        System.out.println("Driver mobile workflow reset complete.");
    }

    private static String requireJdbcUrl() {
        String rawUrl = firstNonBlank(System.getenv("NEON_JDBC_URL"), System.getenv("DATABASE_URL"));
        if (rawUrl == null) {
            throw new IllegalStateException("Missing NEON_JDBC_URL or DATABASE_URL environment variable.");
        }

        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("postgresql://") || rawUrl.startsWith("postgres://")) {
            return "jdbc:" + rawUrl;
        }

        throw new IllegalStateException("Unsupported database URL format. Provide a JDBC PostgreSQL URL.");
    }

    private static String requireEnv(String primaryName, String secondaryName) {
        String value = firstNonBlank(System.getenv(primaryName), System.getenv(secondaryName));
        if (value == null) {
            throw new IllegalStateException("Missing environment variable: " + primaryName);
        }
        return value;
    }

    private static String readSqlFile(String sqlPath) throws IOException {
        return Files.readString(Path.of(sqlPath), StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
