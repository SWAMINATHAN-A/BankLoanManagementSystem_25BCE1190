package com.bankloan.model;

/**
 * Model representing a loan product category from LOAN_TYPES table.
 */
public class LoanType {
    private long loanTypeId;
    private String loanName;
    private String description;
    private double interestRate;
    private double minIncomeRequired;
    private double maxLoanLimit;
    private int maxTenureMonths;
    private double dtiMaxThreshold;
    private String isActive;

    public LoanType() {}

    public LoanType(long loanTypeId, String loanName, String description, double interestRate,
                    double minIncomeRequired, double maxLoanLimit, int maxTenureMonths,
                    double dtiMaxThreshold, String isActive) {
        this.loanTypeId = loanTypeId;
        this.loanName = loanName;
        this.description = description;
        this.interestRate = interestRate;
        this.minIncomeRequired = minIncomeRequired;
        this.maxLoanLimit = maxLoanLimit;
        this.maxTenureMonths = maxTenureMonths;
        this.dtiMaxThreshold = dtiMaxThreshold;
        this.isActive = isActive;
    }

    public long getLoanTypeId() { return loanTypeId; }
    public void setLoanTypeId(long loanTypeId) { this.loanTypeId = loanTypeId; }

    public String getLoanName() { return loanName; }
    public void setLoanName(String loanName) { this.loanName = loanName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public double getMinIncomeRequired() { return minIncomeRequired; }
    public void setMinIncomeRequired(double minIncomeRequired) { this.minIncomeRequired = minIncomeRequired; }

    public double getMaxLoanLimit() { return maxLoanLimit; }
    public void setMaxLoanLimit(double maxLoanLimit) { this.maxLoanLimit = maxLoanLimit; }

    public int getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(int maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }

    public double getDtiMaxThreshold() { return dtiMaxThreshold; }
    public void setDtiMaxThreshold(double dtiMaxThreshold) { this.dtiMaxThreshold = dtiMaxThreshold; }

    public String getIsActive() { return isActive; }
    public void setIsActive(String isActive) { this.isActive = isActive; }
}
