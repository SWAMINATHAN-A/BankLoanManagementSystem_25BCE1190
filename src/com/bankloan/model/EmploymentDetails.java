package com.bankloan.model;

/**
 * Model representing customer employment record from EMPLOYMENT_DETAILS table.
 */
public class EmploymentDetails {
    private long employmentId;
    private long customerId;
    private String employmentType;
    private String employerName;
    private String designation;
    private double monthlyIncome;
    private double experienceYears;
    private String officeAddress;

    public EmploymentDetails() {}

    public long getEmploymentId() { return employmentId; }
    public void setEmploymentId(long employmentId) { this.employmentId = employmentId; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public double getExperienceYears() { return experienceYears; }
    public void setExperienceYears(double experienceYears) { this.experienceYears = experienceYears; }

    public String getOfficeAddress() { return officeAddress; }
    public void setOfficeAddress(String officeAddress) { this.officeAddress = officeAddress; }
}
