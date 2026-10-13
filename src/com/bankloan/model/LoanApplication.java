package com.bankloan.model;

/**
 * Model representing a loan application from LOAN_APPLICATIONS table.
 */
public class LoanApplication {
    private long applicationId;
    private long customerId;
    private long loanTypeId;
    private double requestedAmount;
    private int tenureMonths;
    private double calculatedEligibleAmount;
    private double dtiRatio;
    private String eligibilityOutcome;
    private String outcomeReason;
    private String applicationStatus;
    private String purpose;
    private String appliedAt;
    private String reviewedAt;

    // Joined fields for display
    private String customerName;
    private String loanName;
    private double interestRate;

    public LoanApplication() {}

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public long getLoanTypeId() { return loanTypeId; }
    public void setLoanTypeId(long loanTypeId) { this.loanTypeId = loanTypeId; }

    public double getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(double requestedAmount) { this.requestedAmount = requestedAmount; }

    public int getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(int tenureMonths) { this.tenureMonths = tenureMonths; }

    public double getCalculatedEligibleAmount() { return calculatedEligibleAmount; }
    public void setCalculatedEligibleAmount(double calculatedEligibleAmount) { this.calculatedEligibleAmount = calculatedEligibleAmount; }

    public double getDtiRatio() { return dtiRatio; }
    public void setDtiRatio(double dtiRatio) { this.dtiRatio = dtiRatio; }

    public String getEligibilityOutcome() { return eligibilityOutcome; }
    public void setEligibilityOutcome(String eligibilityOutcome) { this.eligibilityOutcome = eligibilityOutcome; }

    public String getOutcomeReason() { return outcomeReason; }
    public void setOutcomeReason(String outcomeReason) { this.outcomeReason = outcomeReason; }

    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getAppliedAt() { return appliedAt; }
    public void setAppliedAt(String appliedAt) { this.appliedAt = appliedAt; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getLoanName() { return loanName; }
    public void setLoanName(String loanName) { this.loanName = loanName; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }
}
