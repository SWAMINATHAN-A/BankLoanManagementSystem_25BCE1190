
package com.bankloan.service;

import com.bankloan.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SqlDemoService {

    public Map<String, Object> executeDemoSql(String rawSql) {
        Map<String, Object> result = new HashMap<>();

        System.out.println();
        System.out.println("========== SQL DEMO BACKEND ==========");
        System.out.println("[SQL DEMO] Request received");

        if (rawSql == null || rawSql.trim().isEmpty()) {
            System.out.println("[SQL DEMO] ERROR: Empty SQL statement");
            result.put("success", false);
            result.put("message", "SQL statement cannot be empty.");
            return result;
        }

        String sql = rawSql.trim();

        // Strip any trailing semicolons and trailing whitespace iteratively
        // Oracle JDBC throws ORA-00911: invalid character if a trailing semicolon or slash is included
        while (sql.endsWith(";") || sql.endsWith("/")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }

        String upper = sql.toUpperCase();

        System.out.println("[SQL DEMO] SQL received: " + sql);

        // Block destructive operations on protected tables.
        if (upper.contains("DROP TABLE USERS")
                || upper.contains("DROP TABLE CUSTOMERS")
                || upper.contains("DROP TABLE LOAN_APPLICATIONS")
                || upper.contains("DROP TABLE LOAN_TYPES")
                || upper.contains("TRUNCATE")
                || upper.contains("DROP USER")
                || upper.contains("DROP DATABASE")) {

            System.out.println("[SQL DEMO] BLOCKED: Destructive command");

            result.put("success", false);
            result.put("commandType", "REJECTED");
            result.put("message",
                    "Security Block: Destructive system drops or truncates on core schema tables are disallowed in the demonstration console.");

            System.out.println("======================================");
            return result;
        }

        long start = System.currentTimeMillis();

        try (Connection conn = DatabaseConnection.getConnection()) {

            System.out.println("[SQL DEMO] JDBC connection established");
            System.out.println("[SQL DEMO] Database user: "
                    + conn.getMetaData().getUserName());
            System.out.println("[SQL DEMO] Executing SQL in Oracle...");

            if (upper.startsWith("SELECT")) {

                // DQL execution
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    List<String> columns = new ArrayList<>();

                    for (int i = 1; i <= colCount; i++) {
                        columns.add(meta.getColumnLabel(i));
                    }

                    List<List<Object>> rows = new ArrayList<>();
                    int rowLimit = 100;

                    while (rs.next() && rows.size() < rowLimit) {
                        List<Object> row = new ArrayList<>();

                        for (int i = 1; i <= colCount; i++) {
                            row.add(rs.getObject(i));
                        }

                        rows.add(row);
                    }

                    long duration = System.currentTimeMillis() - start;

                    result.put("success", true);
                    result.put("commandType", "DQL");
                    result.put("columns", columns);
                    result.put("rows", rows);
                    result.put("rowCount", rows.size());
                    result.put("executionTimeMs", duration);
                    result.put("message",
                            "Query executed successfully (" + rows.size()
                                    + " rows returned in " + duration + "ms).");

                    System.out.println("[SQL DEMO] Type: DQL");
                    System.out.println("[SQL DEMO] Status: SUCCESS");
                    System.out.println("[SQL DEMO] Columns: " + colCount);
                    System.out.println("[SQL DEMO] Rows returned: " + rows.size());
                    System.out.println("[SQL DEMO] Execution time: "
                            + duration + " ms");
                }

            } else if (upper.startsWith("CREATE")
                    || upper.startsWith("ALTER")
                    || upper.startsWith("DROP")) {

                // DDL execution
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);

                    long duration = System.currentTimeMillis() - start;

                    result.put("success", true);
                    result.put("commandType", "DDL");
                    result.put("executionTimeMs", duration);
                    result.put("message",
                            "DDL command executed successfully in Oracle in "
                                    + duration + "ms.");

                    System.out.println("[SQL DEMO] Type: DDL");
                    System.out.println("[SQL DEMO] Status: SUCCESS");
                    System.out.println("[SQL DEMO] Execution time: "
                            + duration + " ms");
                }

            } else if (upper.startsWith("INSERT")
                    || upper.startsWith("UPDATE")
                    || upper.startsWith("DELETE")
                    || upper.startsWith("MERGE")) {

                // DML execution
                try (Statement stmt = conn.createStatement()) {
                    int affected = stmt.executeUpdate(sql);

                    long duration = System.currentTimeMillis() - start;

                    result.put("success", true);
                    result.put("commandType", "DML");
                    result.put("rowsAffected", affected);
                    result.put("executionTimeMs", duration);
                    result.put("message",
                            "DML statement executed successfully ("
                                    + affected + " rows affected in "
                                    + duration + "ms).");

                    System.out.println("[SQL DEMO] Type: DML");
                    System.out.println("[SQL DEMO] Status: SUCCESS");
                    System.out.println("[SQL DEMO] Rows affected: " + affected);
                    System.out.println("[SQL DEMO] Execution time: "
                            + duration + " ms");
                }

            } else {
                result.put("success", false);
                result.put("commandType", "UNSUPPORTED");
                result.put("message",
                        "Unsupported SQL command type for demonstration console.");

                System.out.println("[SQL DEMO] ERROR: Unsupported command");
            }

        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - start;

            result.put("success", false);
            result.put("errorCode", e.getErrorCode());
            result.put("sqlState", e.getSQLState());
            result.put("executionTimeMs", duration);
            result.put("message",
                    "Oracle Execution Error ORA-" + e.getErrorCode()
                            + ": " + e.getMessage());

            System.out.println("[SQL DEMO] STATUS: FAILED");
            System.out.println("[SQL DEMO] Oracle error code: "
                    + e.getErrorCode());
            System.out.println("[SQL DEMO] Error: " + e.getMessage());
            System.out.println("[SQL DEMO] Execution time: "
                    + duration + " ms");
        }

        System.out.println("======================================");
        System.out.println();

        return result;
    }
}
