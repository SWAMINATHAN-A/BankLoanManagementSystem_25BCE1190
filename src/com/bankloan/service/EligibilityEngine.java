package com.bankloan.service;

import com.bankloan.model.LoanType;
import java.util.HashMap;
import java.util.Map;

/**
 * Transparent, documented educational loan eligibility calculation engine.
 * Computes estimated loan sanction based on income, debt-to-income (DTI) ratio,
 * existing obligations, credit score, and configured product constraints in LOAN_TYPES.
 *
 * Clearly labeled as an EDUCATIONAL ESTIMATE and not an official banking sanction.
 */
public class EligibilityEngine {

    public static class EligibilityResult {
        private final boolean eligible;
        private final String outcome; // ELIGIBLE, CONDITIONAL, INSUFFICIENT_INCOME, HIGH_DTI, INELIGIBLE
        private final double requestedAmount;
        private final double maxEligibleAmount;
        private final double dtiRatio;
        private final double estimatedEmi;
        private final double maxAllowedEmi;
        private final String reason;
        private final String disclaimer;

        public EligibilityResult(boolean eligible, String outcome, double requestedAmount,
                                 double maxEligibleAmount, double dtiRatio, double estimatedEmi,
                                 double maxAllowedEmi, String reason) {
            this.eligible = eligible;
            this.outcome = outcome;
            this.requestedAmount = requestedAmount;
            this.maxEligibleAmount = Math.round(maxEligibleAmount * 100.0) / 100.0;
            this.dtiRatio = Math.round(dtiRatio * 100.0) / 100.0;
            this.estimatedEmi = Math.round(estimatedEmi * 100.0) / 100.0;
            this.maxAllowedEmi = Math.round(maxAllowedEmi * 100.0) / 100.0;
            this.reason = reason;
            this.disclaimer = "EDUCATIONAL ESTIMATE ONLY: This calculation is performed for academic DBMS demonstration purposes and does not represent an actual legal bank commitment or financial sanction.";
        }

        public boolean isEligible() { return eligible; }
        public String getOutcome() { return outcome; }
        public double getRequestedAmount() { return requestedAmount; }
        public double getMaxEligibleAmount() { return maxEligibleAmount; }
        public double getDtiRatio() { return dtiRatio; }
        public double getEstimatedEmi() { return estimatedEmi; }
        public double getMaxAllowedEmi() { return maxAllowedEmi; }
        public String getReason() { return reason; }
        public String getDisclaimer() { return disclaimer; }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("eligible", eligible);
            map.put("outcome", outcome);
            map.put("requestedAmount", requestedAmount);
            map.put("maxEligibleAmount", maxEligibleAmount);
            map.put("dtiRatio", dtiRatio);
            map.put("estimatedEmi", estimatedEmi);
            map.put("maxAllowedEmi", maxAllowedEmi);
            map.put("reason", reason);
            map.put("disclaimer", disclaimer);
            return map;
        }
    }

    /**
     * Calculates educational loan eligibility.
     *
     * Formula Assumptions:
     * 1. Minimum Income Check: Monthly income must meet or exceed loanType.minIncomeRequired.
     * 2. Max Allowed DTI (Debt-To-Income): Configured per loan type (typically 45% - 55%).
     * 3. Max Allowed Total EMI = (Monthly Income * (DTI Threshold / 100)).
     * 4. Available Monthly Disposable Capacity for new loan = Max Allowed Total EMI - Existing Monthly EMIs.
     * 5. Estimated EMI for Requested Loan = Standard PMT Formula:
     *    EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
     *    where r = monthly interest rate, n = tenure in months.
     * 6. Maximum Principal = [Available Monthly Disposable Capacity * ((1 + r)^n - 1)] / [r * (1 + r)^n]
     * 7. Capped by loanType.maxLoanLimit.
     * 8. Credit Score multiplier:
     *    - Score >= 750: 1.0 (Full eligibility)
     *    - Score 650-749: 0.85 (Minor risk buffer)
     *    - Score < 650: 0.60 or rejected if < 600.
     */
    public static EligibilityResult calculateEligibility(
            LoanType loanType,
            double requestedAmount,
            int tenureMonths,
            double monthlyIncome,
            double existingMonthlyEmis,
            double currentBalance,
            double avgMonthlyBalance,
            int creditScore) {

        if (loanType == null) {
            return new EligibilityResult(false, "INELIGIBLE", requestedAmount, 0, 0, 0, 0,
                    "Invalid or unspecified loan product type.");
        }

        // Validate tenure bound
        int tenure = Math.min(tenureMonths, loanType.getMaxTenureMonths());
        if (tenure <= 0) {
            tenure = Math.min(60, loanType.getMaxTenureMonths());
        }

        // 1. Minimum Income Check
        if (monthlyIncome < loanType.getMinIncomeRequired()) {
            return new EligibilityResult(false, "INSUFFICIENT_INCOME", requestedAmount, 0, 0, 0, 0,
                    String.format("Monthly income (₹%.2f) does not satisfy the minimum required threshold of ₹%.2f for %s.",
                            monthlyIncome, loanType.getMinIncomeRequired(), loanType.getLoanName()));
        }

        // 2. Credit Score Assessment
        if (creditScore < 600) {
            return new EligibilityResult(false, "INELIGIBLE", requestedAmount, 0, 0, 0, 0,
                    String.format("Credit score (%d) is below the minimum lending cutoff of 600.", creditScore));
        }

        double scoreMultiplier = 1.0;
        if (creditScore < 700) {
            scoreMultiplier = 0.80;
        } else if (creditScore < 750) {
            scoreMultiplier = 0.90;
        }

        // 3. Compute Monthly Interest Rate
        double annualRate = loanType.getInterestRate();
        double monthlyRate = (annualRate / 12.0) / 100.0;

        // 4. Compute Estimated EMI for Requested Loan Amount
        double requestedEmi = calculateEmi(requestedAmount, monthlyRate, tenure);

        // 5. Compute Debt-to-Income (DTI) with Requested Loan Included
        double totalObligations = existingMonthlyEmis + requestedEmi;
        double calculatedDti = (totalObligations / monthlyIncome) * 100.0;

        // 6. Max Allowed EMI Capacity
        double maxDtiAllowed = loanType.getDtiMaxThreshold();
        double maxAllowedTotalEmi = monthlyIncome * (maxDtiAllowed / 100.0);
        double maxAvailableEmiForNewLoan = maxAllowedTotalEmi - existingMonthlyEmis;

        if (maxAvailableEmiForNewLoan <= 0) {
            return new EligibilityResult(false, "HIGH_DTI", requestedAmount, 0, calculatedDti, requestedEmi, 0,
                    String.format("Existing monthly obligations (₹%.2f) already exceed the maximum allowed Debt-to-Income threshold (%.1f%%).",
                            existingMonthlyEmis, maxDtiAllowed));
        }

        // 7. Calculate Maximum Eligible Principal from available EMI capacity
        double maxPrincipal = calculatePrincipalFromEmi(maxAvailableEmiForNewLoan, monthlyRate, tenure);
        maxPrincipal = maxPrincipal * scoreMultiplier;

        // Cap at Product Maximum
        if (maxPrincipal > loanType.getMaxLoanLimit()) {
            maxPrincipal = loanType.getMaxLoanLimit();
        }

        // 8. Determine Outcome
        if (calculatedDti > maxDtiAllowed || requestedAmount > maxPrincipal) {
            if (maxPrincipal > (requestedAmount * 0.5)) {
                return new EligibilityResult(true, "CONDITIONAL", requestedAmount, maxPrincipal, calculatedDti, requestedEmi, maxAvailableEmiForNewLoan,
                        String.format("Requested amount (₹%.2f) exceeds debt capacity. Applicant is eligible for a reduced sanction up to ₹%.2f (DTI: %.1f%%).",
                                requestedAmount, maxPrincipal, calculatedDti));
            } else {
                return new EligibilityResult(false, "HIGH_DTI", requestedAmount, maxPrincipal, calculatedDti, requestedEmi, maxAvailableEmiForNewLoan,
                        String.format("High Debt-to-Income ratio (%.1f%% exceeds limit %.1f%%). Estimated borrowing capacity is limited to ₹%.2f.",
                                calculatedDti, maxDtiAllowed, maxPrincipal));
            }
        }

        return new EligibilityResult(true, "ELIGIBLE", requestedAmount, maxPrincipal, calculatedDti, requestedEmi, maxAvailableEmiForNewLoan,
                String.format("Applicant satisfies all credit criteria. Requested loan of ₹%.2f is fully supported by cash flow capacity (Max eligibility: ₹%.2f, DTI: %.1f%%).",
                        requestedAmount, maxPrincipal, calculatedDti));
    }

    /**
     * Standard Equal Monthly Installment (EMI) formula:
     * EMI = [P * r * (1 + r)^n] / [(1 + r)^n - 1]
     */
    public static double calculateEmi(double principal, double monthlyRate, int tenureMonths) {
        if (monthlyRate == 0) return principal / tenureMonths;
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return (principal * monthlyRate * factor) / (factor - 1);
    }

    /**
     * Inverse EMI formula to calculate principal supported by an EMI:
     * P = [EMI * ((1 + r)^n - 1)] / [r * (1 + r)^n]
     */
    public static double calculatePrincipalFromEmi(double emi, double monthlyRate, int tenureMonths) {
        if (monthlyRate == 0) return emi * tenureMonths;
        double factor = Math.pow(1 + monthlyRate, tenureMonths);
        return (emi * (factor - 1)) / (monthlyRate * factor);
    }
}
