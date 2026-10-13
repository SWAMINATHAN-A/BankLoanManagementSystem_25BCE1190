package com.bankloan.db;

import com.bankloan.config.DatabaseConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Executes a genuine ODBC connection test by invoking the verified PowerShell ODBC test script.
 * Keeps ODBC testing distinct from JDBC and returns structured diagnostic metrics.
 */
public class OdbcConnectionTester {

    public static Map<String, Object> testOdbcConnection() {
        Map<String, Object> result = new HashMap<>();
        DatabaseConfig cfg = DatabaseConfig.getInstance();

        String dsn = cfg.getOdbcDsn();
        String user = cfg.getOdbcUsername();
        String pass = cfg.getOdbcPassword();

        result.put("dsn", dsn);
        result.put("driver", cfg.getOdbcDriver());

        if (user.isEmpty() || pass.isEmpty()) {
            result.put("success", false);
            result.put("status", "UNCONFIGURED");
            result.put("message", "ODBC credentials not configured. Please supply odbc.username and odbc.password in config/db.properties.");
            return result;
        }

        File scriptFile = new File("sql/test_odbc_connection.ps1");
        if (!scriptFile.exists()) {
            result.put("success", false);
            result.put("status", "SCRIPT_MISSING");
            result.put("message", "ODBC diagnostic script sql/test_odbc_connection.ps1 was not found.");
            return result;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", scriptFile.getAbsolutePath(),
                "-DsnName", dsn,
                "-Username", user,
                "-Password", pass
            );
            pb.redirectErrorStream(true);

            long start = System.currentTimeMillis();
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            long duration = System.currentTimeMillis() - start;

            result.put("exitCode", exitCode);
            result.put("executionTimeMs", duration);
            result.put("rawOutput", output.toString());

            if (exitCode == 0 && output.toString().contains("[ODBC TEST STATUS: PASSED]")) {
                result.put("success", true);
                result.put("status", "CONNECTED");
                result.put("message", "Genuine ODBC connection established successfully through Windows DSN '" + dsn + "'.");
            } else {
                result.put("success", false);
                result.put("status", "FAILED");
                result.put("message", "ODBC connection test failed. Inspect logs for details.");
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("status", "ERROR");
            result.put("message", "Failed to invoke ODBC test runner: " + e.getMessage());
        }

        return result;
    }
}
