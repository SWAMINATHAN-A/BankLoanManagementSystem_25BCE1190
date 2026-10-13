package com.bankloan.handler;

import com.bankloan.model.Customer;
import com.bankloan.model.EmploymentDetails;
import com.bankloan.model.FinancialDetails;
import com.bankloan.model.LoanApplication;
import com.bankloan.service.LoanService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/applications, /api/applications/details, and /api/dashboard.
 */
public class ApplicationHandler implements HttpHandler {

    private final LoanService loanService = new LoanService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            JsonHelper.sendOptionsResponse(exchange);
            return;
        }

        try {
            if (path.endsWith("/dashboard") && "GET".equals(method)) {
                handleDashboard(exchange);
            } else if (path.endsWith("/applications") && "GET".equals(method)) {
                handleGetApplications(exchange);
            } else if (path.endsWith("/applications") && "POST".equals(method)) {
                handleSubmitApplication(exchange);
            } else if (path.contains("/applications/details") && "GET".equals(method)) {
                handleGetApplicationDetails(exchange);
            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Endpoint not supported: " + path);
                JsonHelper.sendJsonResponse(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Application error: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }

    private void handleDashboard(HttpExchange exchange) throws IOException, SQLException {
        Map<String, Object> stats = loanService.getDashboardStats();
        stats.put("success", true);
        JsonHelper.sendJsonResponse(exchange, 200, stats);
    }

    private void handleGetApplications(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getQuery();
        String status = "ALL";
        String search = "";

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    if ("status".equalsIgnoreCase(pair[0])) status = java.net.URLDecoder.decode(pair[1], "UTF-8");
                    if ("search".equalsIgnoreCase(pair[0])) search = java.net.URLDecoder.decode(pair[1], "UTF-8");
                }
            }
        }

        List<LoanApplication> apps = loanService.getApplications(status, search);
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (LoanApplication app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("applicationId", app.getApplicationId());
            m.put("customerId", app.getCustomerId());
            m.put("customerName", app.getCustomerName());
            m.put("loanName", app.getLoanName());
            m.put("interestRate", app.getInterestRate());
            m.put("requestedAmount", app.getRequestedAmount());
            m.put("tenureMonths", app.getTenureMonths());
            m.put("calculatedEligibleAmount", app.getCalculatedEligibleAmount());
            m.put("dtiRatio", app.getDtiRatio());
            m.put("eligibilityOutcome", app.getEligibilityOutcome());
            m.put("outcomeReason", app.getOutcomeReason());
            m.put("applicationStatus", app.getApplicationStatus());
            m.put("purpose", app.getPurpose());
            m.put("appliedAt", app.getAppliedAt());
            mapped.add(m);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("total", mapped.size());
        res.put("applications", mapped);
        JsonHelper.sendJsonResponse(exchange, 200, res);
    }

    private void handleGetApplicationDetails(HttpExchange exchange) throws IOException, SQLException {
        String query = exchange.getRequestURI().getQuery();
        long appId = -1;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "id".equalsIgnoreCase(pair[0])) {
                    try { appId = Long.parseLong(pair[1]); } catch (NumberFormatException ignored) {}
                }
            }
        }

        if (appId <= 0) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Application ID is required.");
            JsonHelper.sendJsonResponse(exchange, 400, err);
            return;
        }

        Map<String, Object> details = loanService.getApplicationDetails(appId);
        if (details == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Application #" + appId + " not found.");
            JsonHelper.sendJsonResponse(exchange, 404, err);
            return;
        }

        details.put("success", true);
        JsonHelper.sendJsonResponse(exchange, 200, details);
    }

    private void handleSubmitApplication(HttpExchange exchange) throws IOException {
        String body = JsonHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        try {
            // Customer mapping
            Customer cust = new Customer();
            cust.setFullName(data.getOrDefault("fullName", ""));
            cust.setEmail(data.getOrDefault("email", ""));
            cust.setPhone(data.getOrDefault("phone", ""));
            cust.setAge(Integer.parseInt(data.getOrDefault("age", "25")));
            cust.setGender(data.getOrDefault("gender", "MALE"));
            cust.setPanNumber(data.getOrDefault("panNumber", ""));
            cust.setAddress(data.getOrDefault("address", ""));

            // Employment mapping
            EmploymentDetails emp = new EmploymentDetails();
            emp.setEmploymentType(data.getOrDefault("employmentType", "SALARIED"));
            emp.setEmployerName(data.getOrDefault("employerName", ""));
            emp.setDesignation(data.getOrDefault("designation", ""));
            emp.setMonthlyIncome(Double.parseDouble(data.getOrDefault("monthlyIncome", "0")));
            emp.setExperienceYears(Double.parseDouble(data.getOrDefault("experienceYears", "1")));
            emp.setOfficeAddress(data.getOrDefault("officeAddress", ""));

            // Financial mapping
            FinancialDetails fin = new FinancialDetails();
            fin.setCurrentBalance(Double.parseDouble(data.getOrDefault("currentBalance", "0")));
            fin.setAvgMonthlyBalance(Double.parseDouble(data.getOrDefault("avgMonthlyBalance", "0")));
            fin.setMonthlyTurnover(Double.parseDouble(data.getOrDefault("monthlyTurnover", "0")));
            fin.setYearlyTurnover(Double.parseDouble(data.getOrDefault("yearlyTurnover", "0")));
            fin.setExistingMonthlyEmis(Double.parseDouble(data.getOrDefault("existingMonthlyEmis", "0")));
            fin.setCreditScore(Integer.parseInt(data.getOrDefault("creditScore", "750")));

            // Application params
            long loanTypeId = Long.parseLong(data.getOrDefault("loanTypeId", "1"));
            double requestedAmount = Double.parseDouble(data.getOrDefault("requestedAmount", "100000"));
            int tenureMonths = Integer.parseInt(data.getOrDefault("tenureMonths", "36"));
            String purpose = data.getOrDefault("purpose", "General personal and family requirements");

            // Server-side validation
            if (cust.getFullName().isEmpty() || cust.getEmail().isEmpty() || cust.getPanNumber().isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Full name, email, and PAN number are mandatory fields.");
                JsonHelper.sendJsonResponse(exchange, 400, err);
                return;
            }

            Map<String, Object> result = loanService.submitLoanApplication(
                cust, emp, fin, loanTypeId, requestedAmount, tenureMonths, purpose
            );

            JsonHelper.sendJsonResponse(exchange, 201, result);

        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Validation error: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 400, err);
        } catch (SQLException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Database transaction failed: ORA-" + e.getErrorCode() + ": " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }
}
