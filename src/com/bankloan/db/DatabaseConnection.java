package com.bankloan.db;

import com.bankloan.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages Oracle JDBC database connectivity, transactions, and diagnostics.
 */
public class DatabaseConnection {

    static {
        try {
            // Explicitly load Oracle JDBC Driver
            Class.forName(DatabaseConfig.getInstance().getDbDriver());
            System.out.println("[JDBC] Oracle JDBC Driver registered successfully: " + DatabaseConfig.getInstance().getDbDriver());
        } catch (ClassNotFoundException e) {
            System.err.println("[JDBC ERROR] Failed to load Oracle JDBC Driver. Ensure lib/ojdbc6.jar is in the classpath: " + e.getMessage());
        }
    }

    /**
     * Obtains a new database connection using credentials from DatabaseConfig.
     */
    public static Connection getConnection() throws SQLException {
        DatabaseConfig cfg = DatabaseConfig.getInstance();
        return DriverManager.getConnection(cfg.getDbUrl(), cfg.getDbUsername(), cfg.getDbPassword());
    }

    /**
     * Executes genuine JDBC diagnostics against Oracle and returns safe metadata.
     * Never exposes credentials or passwords.
     */
    public static Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        DatabaseConfig cfg = DatabaseConfig.getInstance();

        if (cfg.getDbUsername().isEmpty() || cfg.getDbPassword().isEmpty()) {
            result.put("success", false);
            result.put("status", "UNCONFIGURED");
            result.put("message", "Database credentials not configured in config/db.properties. Please configure db.username and db.password.");
            return result;
        }

        long start = System.currentTimeMillis();
        try (Connection conn = getConnection()) {
            long duration = System.currentTimeMillis() - start;
            DatabaseMetaData meta = conn.getMetaData();

            result.put("success", true);
            result.put("status", "CONNECTED");
            result.put("responseTimeMs", duration);
            result.put("databaseProduct", meta.getDatabaseProductName());
            result.put("databaseVersion", meta.getDatabaseProductVersion());
            result.put("driverName", meta.getDriverName());
            result.put("driverVersion", meta.getDriverVersion());
            result.put("jdbcUrl", cfg.getDbUrl());

            // Run sample verification query against Oracle dual
            try (PreparedStatement stmt = conn.prepareStatement("SELECT sys_context('USERENV', 'CURRENT_USER') AS curr_user, sys_context('USERENV', 'DB_NAME') AS db_name FROM dual");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.put("connectedUser", rs.getString("curr_user"));
                    result.put("databaseName", rs.getString("db_name"));
                }
            }

            // Quick table counts
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) AS total_tables FROM user_tables");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.put("userTablesCount", rs.getInt("total_tables"));
                }
            }

            System.out.println("[JDBC TEST SUCCESS] Connected to Oracle XE in " + duration + "ms as user " + result.get("connectedUser"));

        } catch (SQLException e) {
            result.put("success", false);
            result.put("status", "CONNECTION_FAILED");
            result.put("errorCode", e.getErrorCode());
            result.put("sqlState", e.getSQLState());
            result.put("message", e.getMessage());
            System.err.println("[JDBC TEST ERROR] Oracle connection failed: ORA-" + e.getErrorCode() + ": " + e.getMessage());
        }

        return result;
    }
}
