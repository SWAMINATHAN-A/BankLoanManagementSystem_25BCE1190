
package com.bankloan.handler;

import com.bankloan.service.SqlDemoService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SqlDemoHandler implements HttpHandler {

    private final SqlDemoService sqlDemoService = new SqlDemoService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod().toUpperCase();

        System.out.println();
        System.out.println("[SQL HANDLER] Request received");
        System.out.println("[SQL HANDLER] Endpoint: /api/sql-demo");
        System.out.println("[SQL HANDLER] HTTP method: " + method);

        if ("OPTIONS".equals(method)) {
            System.out.println("[SQL HANDLER] Handling OPTIONS request");
            JsonHelper.sendOptionsResponse(exchange);
            return;
        }

        if (!"POST".equals(method)) {
            Map<String, Object> err = new HashMap<>();

            err.put("success", false);
            err.put("message", "Method not allowed. Use POST.");

            System.out.println("[SQL HANDLER] ERROR: POST required");

            JsonHelper.sendJsonResponse(exchange, 405, err);
            return;
        }

        try {
            String body = JsonHelper.readRequestBody(exchange);
            Map<String, String> data = JsonHelper.parseSimpleJson(body);
            String sql = data.get("sql");

            if (sql == null || sql.trim().isEmpty()) {
                Map<String, Object> err = new HashMap<>();

                err.put("success", false);
                err.put("message",
                        "Field 'sql' is required in request body.");

                System.out.println("[SQL HANDLER] ERROR: SQL field missing");

                JsonHelper.sendJsonResponse(exchange, 400, err);
                return;
            }

            System.out.println("[SQL HANDLER] SQL request forwarded to service");

            Map<String, Object> result =
                    sqlDemoService.executeDemoSql(sql);

            int status = Boolean.TRUE.equals(result.get("success"))
                    ? 200 : 400;

            System.out.println("[SQL HANDLER] HTTP response status: " + status);

            JsonHelper.sendJsonResponse(exchange, status, result);

            System.out.println("[SQL HANDLER] Response sent to website");

        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();

            err.put("success", false);
            err.put("message",
                    "SQL execution service exception: " + e.getMessage());

            System.out.println("[SQL HANDLER] UNEXPECTED ERROR: "
                    + e.getMessage());

            e.printStackTrace();

            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }
}
