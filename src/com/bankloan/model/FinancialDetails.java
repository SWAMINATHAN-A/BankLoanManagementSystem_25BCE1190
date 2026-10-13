package com.bankloan.model;

/**
 * Model representing customer banking health from FINANCIAL_DETAILS table.
 */
public class FinancialDetails {
    private long financialId;
    private long customerId;
    private double currentBalance;
    private double avgMonthlyBalance;
    private double monthlyTurnover;
    private double yearlyTurnover;
    private double existingMonthlyEmis;
    private int creditScore;
    private String updatedAt;

    public FinancialDetails() {}

    public long getFinancialId() { return financialId; }
    public void setFinancialId(long financialId) { this.financialId = financialId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public double getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(double currentBalance) { this.currentBalance = currentBalance; }

    public double getAvgMonthlyBalance() { return avgMonthlyBalance; }
    public void setAvgMonthlyBalance(double avgMonthlyBalance) { this.avgMonthlyBalance = avgMonthlyBalance; }

    public double getMonthlyTurnover() { return monthlyTurnover; }
    public void setMonthlyTurnover(double monthlyTurnover) { this.monthlyTurnover = monthlyTurnover; }

    public double getYearlyTurnover() { return yearlyTurnover; }
    public void setYearlyTurnover(double yearlyTurnover) { this.yearlyTurnover = yearlyTurnover; }

    public double getExistingMonthlyEmis() { return existingMonthlyEmis; }
    public void setExistingMonthlyEmis(double existingMonthlyEmis) { this.existingMonthlyEmis = existingMonthlyEmis; }

    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
