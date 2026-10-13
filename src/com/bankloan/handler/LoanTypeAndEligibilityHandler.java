package com.bankloan.handler;

import com.bankloan.model.LoanType;
import com.bankloan.service.EligibilityEngine;
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
 * Handles /api/loan-types and /api/eligibility/calculate endpoints.
 */
public class LoanTypeAndEligibilityHandler implements HttpHandler {

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
            if (path.endsWith("/loan-types") && "GET".equals(method)) {
                handleGetLoanTypes(exchange);
            } else if (path.endsWith("/calculate") && "POST".equals(method)) {
                handleCalculateEligibility(exchange);
            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Endpoint not supported: " + path);
                JsonHelper.sendJsonResponse(exchange, 404, err);
            }
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Error processing request: " + e.getMessage());
            JsonHelper.sendJsonResponse(exchange, 500, err);
        }
    }

    private void handleGetLoanTypes(HttpExchange exchange) throws IOException, SQLException {
        List<LoanType> list = loanService.getLoanTypes();
        List<Map<String, Object>> mapped = new ArrayList<>();
        for (LoanType lt : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("loanTypeId", lt.getLoanTypeId());
            m.put("loanName", lt.getLoanName());
            m.put("description", lt.getDescription());
            m.put("interestRate", lt.getInterestRate());
            m.put("minIncomeRequired", lt.getMinIncomeRequired());
            m.put("maxLoanLimit", lt.getMaxLoanLimit());
            m.put("maxTenureMonths", lt.getMaxTenureMonths());
            m.put("dtiMaxThreshold", lt.getDtiMaxThreshold());
            mapped.add(m);
        }
        JsonHelper.sendJsonResponse(exchange, 200, mapped);
    }

    private void handleCalculateEligibility(HttpExchange exchange) throws IOException, SQLException {
        String body = JsonHelper.readRequestBody(exchange);
        Map<String, String> params = JsonHelper.parseSimpleJson(body);

        long loanTypeId = Long.parseLong(params.getOrDefault("loanTypeId", "1"));
        double requestedAmount = Double.parseDouble(params.getOrDefault("requestedAmount", "1000000"));
        int tenureMonths = Integer.parseInt(params.getOrDefault("tenureMonths", "60"));
        double monthlyIncome = Double.parseDouble(params.getOrDefault("monthlyIncome", "50000"));
        double existingMonthlyEmis = Double.parseDouble(params.getOrDefault("existingMonthlyEmis", "0"));
        double currentBalance = Double.parseDouble(params.getOrDefault("currentBalance", "0"));
        double avgMonthlyBalance = Double.parseDouble(params.getOrDefault("avgMonthlyBalance", "0"));
        int creditScore = Integer.parseInt(params.getOrDefault("creditScore", "750"));

        LoanType loanType = loanService.getLoanTypeById(loanTypeId);
        if (loanType == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Loan type ID not found: " + loanTypeId);
            JsonHelper.sendJsonResponse(exchange, 404, err);
            return;
        }

        EligibilityEngine.EligibilityResult result = EligibilityEngine.calculateEligibility(
            loanType,
            requestedAmount,
            tenureMonths,
            monthlyIncome,
            existingMonthlyEmis,
            currentBalance,
            avgMonthlyBalance,
            creditScore
        );

        Map<String, Object> res = result.toMap();
        res.put("success", true);
        res.put("loanName", loanType.getLoanName());
        res.put("interestRate", loanType.getInterestRate());

        JsonHelper.sendJsonResponse(exchange, 200, res);
    }
}
