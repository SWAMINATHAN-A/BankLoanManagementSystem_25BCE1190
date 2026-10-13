package com.bankloan;

import com.bankloan.config.DatabaseConfig;
import com.bankloan.db.DatabaseConnection;
import com.bankloan.handler.ApplicationHandler;
import com.bankloan.handler.AuthHandler;
import com.bankloan.handler.ConnectivityHandler;
import com.bankloan.handler.JsonHelper;
import com.bankloan.handler.LoanTypeAndEligibilityHandler;
import com.bankloan.handler.SqlDemoHandler;
import com.bankloan.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.concurrent.Executors;

/**
 * Main application entry point.
 * Initializes the com.sun.net.httpserver.HttpServer instance, binds routes,
 * ensures an initial PBKDF2 administrator exists, and handles static assets.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  Bank Loan Management System — DBMS PAT-4 Backend Server");
        System.out.println("==================================================================");

        DatabaseConfig config = DatabaseConfig.getInstance();
        int port = config.getServerPort();

        // 1. Initial Database Check and Admin Account Provisioning
        try {
            System.out.println("[STARTUP] Testing initial Oracle JDBC connection...");
            var test = DatabaseConnection.testConnection();
            if (Boolean.TRUE.equals(test.get("success"))) {
                System.out.println("[STARTUP] Oracle connected as " + test.get("connectedUser") + " (" + test.get("userTablesCount") + " tables detected)");
                
                // Ensure default admin (admin / admin123) is provisioned with salt + PBKDF2
                UserService userService = new UserService();
                userService.provisionUser("admin", "admin123", "System Administrator", "ADMIN");
                userService.provisionUser("officer1", "officer123", "Credit Officer Rajesh", "OFFICER");
            } else {
                System.err.println("[STARTUP WARNING] Database test returned: " + test.get("message"));
                System.err.println("[STARTUP WARNING] Server will start, but database endpoints will report connection failure until Oracle credentials in config/db.properties are verified.");
            }
        } catch (SQLException e) {
            System.err.println("[STARTUP EXCEPTION] Could not verify database during startup: " + e.getMessage());
        }

        // 2. Start HTTP Server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Register API Handlers
            server.createContext("/api/health", new ConnectivityHandler());
            server.createContext("/api/test-jdbc", new ConnectivityHandler());
            server.createContext("/api/test-odbc", new ConnectivityHandler());

            server.createContext("/api/login", new AuthHandler());
            server.createContext("/api/logout", new AuthHandler());
            server.createContext("/api/session", new AuthHandler());

            server.createContext("/api/loan-types", new LoanTypeAndEligibilityHandler());
            server.createContext("/api/eligibility/calculate", new LoanTypeAndEligibilityHandler());

            server.createContext("/api/dashboard", new ApplicationHandler());
            server.createContext("/api/applications", new ApplicationHandler());
            server.createContext("/api/applications/details", new ApplicationHandler());

            server.createContext("/api/sql-demo", new SqlDemoHandler());

            // Static File Serving Handler for web/
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();

            System.out.println("[SERVER READY] HTTP Server is listening on port " + port);
            System.out.println("[SERVER READY] Web URL: http://localhost:" + port);
            System.out.println("==================================================================");

        } catch (IOException e) {
            System.err.println("[SERVER FATAL] Failed to start HTTP server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Serves frontend files from web/ directory with proper MIME types.
     */
    static class StaticFileHandler implements HttpHandler {
        private final File webRoot = new File("web");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // Default home redirect
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File requestedFile = new File(webRoot, path);

            if (!requestedFile.exists() || requestedFile.isDirectory()) {
                // If the frontend file does not exist yet (during Batch 2), return clean JSON status
                String fallbackResponse = "{\"status\":\"ONLINE\",\"message\":\"Bank Loan Management Backend API is running. Batch 2 is active.\",\"requestedPath\":\"" + path + "\"}";
                byte[] bytes = fallbackResponse.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            String contentType = getContentType(requestedFile.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, requestedFile.length());

            try (FileInputStream fis = new FileInputStream(requestedFile);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private String getContentType(String filename) {
            if (filename.endsWith(".html")) return "text/html; charset=UTF-8";
            if (filename.endsWith(".css")) return "text/css; charset=UTF-8";
            if (filename.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (filename.endsWith(".json")) return "application/json; charset=UTF-8";
            if (filename.endsWith(".png")) return "image/png";
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
            if (filename.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
    }
}
