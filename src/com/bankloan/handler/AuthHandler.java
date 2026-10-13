package com.bankloan.handler;

import com.bankloan.db.DatabaseConnection;
import com.bankloan.model.User;
import com.bankloan.security.SessionManager;
import com.bankloan.service.UserService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles /api/login, /api/logout, and /api/session authentication endpoints.
 */
public class AuthHandler implements HttpHandler {

    private final UserService userService = new UserService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            JsonHelper.sendOptionsResponse(exchange);
            return;
        }

        try {
            if (path.endsWith("/login") && "POST".equals(method)) {
                handleLogin(exchange);
            } else if (path.endsWith("/logout") && "POST".equals(method)) {
                handleLogout(exchange);
            } else if (path.endsWith("/session") && "GET".equals(method)) {
                handleCheckSession(exchange);
            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Endpoint not supported");
                JsonHelper.sendJsonResponse(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Internal server error: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = JsonHelper.readRequestBody(exchange);
        Map<String, String> creds = JsonHelper.parseSimpleJson(body);

        String username = creds.get("username");
        String password = creds.get("password");

        Map<String, Object> res = new HashMap<>();
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            res.put("success", false);
            res.put("message", "Username and password are required.");
            JsonHelper.sendJsonResponse(exchange, 400, res);
            return;
        }

        try {
            User user = userService.authenticate(username, password);
            if (user != null) {
                SessionManager.Session session = SessionManager.getInstance().createSession(
                    user.getUserId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole()
                );

                res.put("success", true);
                res.put("message", "Authentication successful.");
                res.put("token", session.getSessionId());
                res.put("userId", user.getUserId());
                res.put("username", user.getUsername());
                res.put("fullName", user.getFullName());
                res.put("role", user.getRole());

                JsonHelper.sendJsonResponse(exchange, 200, res);
            } else {
                res.put("success", false);
                res.put("message", "Invalid username or password.");
                JsonHelper.sendJsonResponse(exchange, 401, res);
            }
        } catch (SQLException e) {
            res.put("success", false);
            res.put("message", "Database error during login: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, res);
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
        if (token != null) {
            SessionManager.getInstance().invalidateSession(token);
        }
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Logged out successfully.");
        JsonHelper.sendJsonResponse(exchange, 200, res);
    }

    private void handleCheckSession(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Session-Token");
        SessionManager.Session session = SessionManager.getInstance().getValidSession(token);

        Map<String, Object> res = new HashMap<>();
        if (session != null) {
            res.put("authenticated", true);
            res.put("username", session.getUsername());
            res.put("fullName", session.getFullName());
            res.put("role", session.getRole());
            JsonHelper.sendJsonResponse(exchange, 200, res);
        } else {
            res.put("authenticated", false);
            JsonHelper.sendJsonResponse(exchange, 401, res);
        }
    }
}
