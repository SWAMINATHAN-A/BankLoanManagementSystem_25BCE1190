package com.bankloan.handler;

import com.bankloan.db.DatabaseConnection;
import com.bankloan.db.OdbcConnectionTester;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles /api/health, /api/test-jdbc, and /api/test-odbc diagnostic endpoints.
 */
public class ConnectivityHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            JsonHelper.sendOptionsResponse(exchange);
            return;
        }

        try {
            if (path.endsWith("/health")) {
                Map<String, Object> health = new HashMap<>();
                health.put("status", "UP");
                health.put("service", "Bank Loan Management System");
                health.put("timestamp", System.currentTimeMillis());
                health.put("version", "1.0.0-PAT4");
                JsonHelper.sendJsonResponse(exchange, 200, health);

            } else if (path.endsWith("/test-jdbc")) {
                Map<String, Object> jdbcResult = DatabaseConnection.testConnection();
                int code = (Boolean.TRUE.equals(jdbcResult.get("success"))) ? 200 : 500;
                JsonHelper.sendJsonResponse(exchange, code, jdbcResult);

            } else if (path.endsWith("/test-odbc")) {
                Map<String, Object> odbcResult = OdbcConnectionTester.testOdbcConnection();
                int code = (Boolean.TRUE.equals(odbcResult.get("success"))) ? 200 : 500;
                JsonHelper.sendJsonResponse(exchange, code, odbcResult);

            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Endpoint not found: " + path);
                JsonHelper.sendJsonResponse(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Connectivity diagnostic error: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }
}
