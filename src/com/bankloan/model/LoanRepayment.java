package com.bankloan.model;

/**
 * Model representing repayment schedule entries from LOAN_REPAYMENTS table.
 */
public class LoanRepayment {
    private long repaymentId;
    private long applicationId;
    private int installmentNo;
    private String dueDate;
    private double emiAmount;
    private double principalComponent;
    private double interestComponent;
    private double remainingBalance;
    private String paymentStatus;
    private String paidDate;

    public LoanRepayment() {}

    public long getRepaymentId() { return repaymentId; }
    public void setRepaymentId(long repaymentId) { this.repaymentId = repaymentId; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public int getInstallmentNo() { return installmentNo; }
    public void setInstallmentNo(int installmentNo) { this.installmentNo = installmentNo; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public double getEmiAmount() { return emiAmount; }
    public void setEmiAmount(double emiAmount) { this.emiAmount = emiAmount; }

    public double getPrincipalComponent() { return principalComponent; }
    public void setPrincipalComponent(double principalComponent) { this.principalComponent = principalComponent; }

    public double getInterestComponent() { return interestComponent; }
    public void setInterestComponent(double interestComponent) { this.interestComponent = interestComponent; }

    public double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(double remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
}
