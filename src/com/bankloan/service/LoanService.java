package com.bankloan.service;

import com.bankloan.db.DatabaseConnection;
import com.bankloan.model.Customer;
import com.bankloan.model.EmploymentDetails;
import com.bankloan.model.FinancialDetails;
import com.bankloan.model.LoanApplication;
import com.bankloan.model.LoanDocument;
import com.bankloan.model.LoanRepayment;
import com.bankloan.model.LoanType;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing transactional loan operations against Oracle 11g XE.
 * Uses atomic multi-table transactions (commit / rollback), sequences,
 * and prepared statements throughout.
 */
public class LoanService {

    /**
     * Retrieves all active loan types from LOAN_TYPES.
     */
    public List<LoanType> getLoanTypes() throws SQLException {
        List<LoanType> list = new ArrayList<>();
        String sql = "SELECT loan_type_id, loan_name, description, interest_rate, " +
                     "min_income_required, max_loan_limit, max_tenure_months, dti_max_threshold, is_active " +
                     "FROM LOAN_TYPES WHERE is_active = 'Y' ORDER BY loan_type_id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                LoanType lt = new LoanType(
                    rs.getLong("loan_type_id"),
                    rs.getString("loan_name"),
                    rs.getString("description"),
                    rs.getDouble("interest_rate"),
                    rs.getDouble("min_income_required"),
                    rs.getDouble("max_loan_limit"),
                    rs.getInt("max_tenure_months"),
                    rs.getDouble("dti_max_threshold"),
                    rs.getString("is_active")
                );
                list.add(lt);
            }
        }
        return list;
    }

    /**
     * Retrieves a single loan type by ID.
     */
    public LoanType getLoanTypeById(long loanTypeId) throws SQLException {
        String sql = "SELECT loan_type_id, loan_name, description, interest_rate, " +
                     "min_income_required, max_loan_limit, max_tenure_months, dti_max_threshold, is_active " +
                     "FROM LOAN_TYPES WHERE loan_type_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, loanTypeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new LoanType(
                        rs.getLong("loan_type_id"),
                        rs.getString("loan_name"),
                        rs.getString("description"),
                        rs.getDouble("interest_rate"),
                        rs.getDouble("min_income_required"),
                        rs.getDouble("max_loan_limit"),
                        rs.getInt("max_tenure_months"),
                        rs.getDouble("dti_max_threshold"),
                        rs.getString("is_active")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Submits a full loan application in an ATOMIC transaction spanning:
     * 1. CUSTOMERS (seq_customers)
     * 2. EMPLOYMENT_DETAILS (seq_employment)
     * 3. FINANCIAL_DETAILS (seq_financial)
     * 4. LOAN_APPLICATIONS (seq_applications)
     * 5. LOAN_DOCUMENTS (seq_documents) - auto-initializes standard compliance checklist
     * 6. LOAN_REPAYMENTS (seq_repayments) - auto-generates demonstration amortization table if ELIGIBLE
     */
    public Map<String, Object> submitLoanApplication(
            Customer customer,
            EmploymentDetails employment,
            FinancialDetails financial,
            long loanTypeId,
            double requestedAmount,
            int tenureMonths,
            String purpose) throws SQLException {

        Map<String, Object> response = new HashMap<>();

        // 1. Fetch Loan Type
        LoanType loanType = getLoanTypeById(loanTypeId);
        if (loanType == null) {
            throw new IllegalArgumentException("Specified Loan Product ID " + loanTypeId + " does not exist.");
        }

        // 2. Perform Eligibility Engine Calculation
        EligibilityEngine.EligibilityResult eligibility = EligibilityEngine.calculateEligibility(
            loanType,
            requestedAmount,
            tenureMonths,
            employment.getMonthlyIncome(),
            financial.getExistingMonthlyEmis(),
            financial.getCurrentBalance(),
            financial.getAvgMonthlyBalance(),
            financial.getCreditScore()
        );

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Atomic Transaction

            // Step 1: Insert CUSTOMERS using seq_customers
            long customerId;
            String sqlCustSeq = "SELECT seq_customers.NEXTVAL FROM dual";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCustSeq);
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                customerId = rs.getLong(1);
            }

            String sqlCust = "INSERT INTO CUSTOMERS (customer_id, user_id, full_name, email, phone, age, gender, pan_number, address) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCust)) {
                stmt.setLong(1, customerId);
                stmt.setNull(2, java.sql.Types.NUMERIC); // user_id optional
                stmt.setString(3, customer.getFullName());
                stmt.setString(4, customer.getEmail());
                stmt.setString(5, customer.getPhone());
                stmt.setInt(6, customer.getAge());
                stmt.setString(7, customer.getGender());
                stmt.setString(8, customer.getPanNumber());
                stmt.setString(9, customer.getAddress());
                stmt.executeUpdate();
            }

            // Step 2: Insert EMPLOYMENT_DETAILS using seq_employment
            long empId;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT seq_employment.NEXTVAL FROM dual");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                empId = rs.getLong(1);
            }

            String sqlEmp = "INSERT INTO EMPLOYMENT_DETAILS (employment_id, customer_id, employment_type, employer_name, designation, monthly_income, experience_years, office_address) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlEmp)) {
                stmt.setLong(1, empId);
                stmt.setLong(2, customerId);
                stmt.setString(3, employment.getEmploymentType());
                stmt.setString(4, employment.getEmployerName());
                stmt.setString(5, employment.getDesignation());
                stmt.setDouble(6, employment.getMonthlyIncome());
                stmt.setDouble(7, employment.getExperienceYears());
                stmt.setString(8, employment.getOfficeAddress());
                stmt.executeUpdate();
            }

            // Step 3: Insert FINANCIAL_DETAILS using seq_financial
            long finId;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT seq_financial.NEXTVAL FROM dual");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                finId = rs.getLong(1);
            }

            String sqlFin = "INSERT INTO FINANCIAL_DETAILS (financial_id, customer_id, current_balance, avg_monthly_balance, monthly_turnover, yearly_turnover, existing_monthly_emis, credit_score) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlFin)) {
                stmt.setLong(1, finId);
                stmt.setLong(2, customerId);
                stmt.setDouble(3, financial.getCurrentBalance());
                stmt.setDouble(4, financial.getAvgMonthlyBalance());
                stmt.setDouble(5, financial.getMonthlyTurnover());
                stmt.setDouble(6, financial.getYearlyTurnover());
                stmt.setDouble(7, financial.getExistingMonthlyEmis());
                stmt.setInt(8, financial.getCreditScore());
                stmt.executeUpdate();
            }

            // Step 4: Insert LOAN_APPLICATIONS using seq_applications
            long appId;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT seq_applications.NEXTVAL FROM dual");
                 ResultSet rs = stmt.executeQuery()) {
                rs.next();
                appId = rs.getLong(1);
            }

            String appStatus = eligibility.isEligible() ? "UNDER_REVIEW" : "REJECTED";
            String sqlApp = "INSERT INTO LOAN_APPLICATIONS (application_id, customer_id, loan_type_id, requested_amount, tenure_months, " +
                            "calculated_eligible_amount, dti_ratio, eligibility_outcome, outcome_reason, application_status, purpose) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sqlApp)) {
                stmt.setLong(1, appId);
                stmt.setLong(2, customerId);
                stmt.setLong(3, loanTypeId);
                stmt.setDouble(4, requestedAmount);
                stmt.setInt(5, tenureMonths);
                stmt.setDouble(6, eligibility.getMaxEligibleAmount());
                stmt.setDouble(7, eligibility.getDtiRatio());
                stmt.setString(8, eligibility.getOutcome());
                stmt.setString(9, eligibility.getReason());
                stmt.setString(10, appStatus);
                stmt.setString(11, purpose);
                stmt.executeUpdate();
            }

            // Step 5: Insert LOAN_DOCUMENTS (Mandatory KYC/Income checklist)
            String[] docNames = {"PAN Card Copy", "Proof of Income / Salary Slip", "Bank Statement (Last 6 Months)"};
            String[] docTypes = {"IDENTITY_PROOF", "INCOME_PROOF", "BANK_STATEMENT"};

            String sqlDoc = "INSERT INTO LOAN_DOCUMENTS (document_id, application_id, document_name, document_type, verification_status, remarks) " +
                            "VALUES (seq_documents.NEXTVAL, ?, ?, ?, 'PENDING', 'Uploaded with application submission')";
            try (PreparedStatement stmt = conn.prepareStatement(sqlDoc)) {
                for (int i = 0; i < docNames.length; i++) {
                    stmt.setLong(1, appId);
                    stmt.setString(2, docNames[i]);
                    stmt.setString(3, docTypes[i]);
                    stmt.executeUpdate();
                }
            }

            // Step 6: If application outcome is positive, generate first 5 demonstration repayment installments
            if (eligibility.isEligible()) {
                double principal = Math.min(requestedAmount, eligibility.getMaxEligibleAmount());
                double monthlyRate = (loanType.getInterestRate() / 12.0) / 100.0;
                double emi = EligibilityEngine.calculateEmi(principal, monthlyRate, tenureMonths);
                double balance = principal;

                String sqlRepay = "INSERT INTO LOAN_REPAYMENTS (repayment_id, application_id, installment_no, due_date, emi_amount, principal_component, interest_component, remaining_balance, payment_status) " +
                                  "VALUES (seq_repayments.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, 'PENDING')";

                long nowMillis = System.currentTimeMillis();
                try (PreparedStatement stmt = conn.prepareStatement(sqlRepay)) {
                    int installmentsToDemo = Math.min(6, tenureMonths);
                    for (int inst = 1; inst <= installmentsToDemo; inst++) {
                        double interestComp = balance * monthlyRate;
                        double principalComp = emi - interestComp;
                        balance = Math.max(0, balance - principalComp);

                        // Monthly due dates: now + inst * 30 days
                        Date dueDate = new Date(nowMillis + ((long) inst * 30L * 24L * 60L * 60L * 1000L));

                        stmt.setLong(1, appId);
                        stmt.setInt(2, inst);
                        stmt.setDate(3, dueDate);
                        stmt.setDouble(4, Math.round(emi * 100.0) / 100.0);
                        stmt.setDouble(5, Math.round(principalComp * 100.0) / 100.0);
                        stmt.setDouble(6, Math.round(interestComp * 100.0) / 100.0);
                        stmt.setDouble(7, Math.round(balance * 100.0) / 100.0);
                        stmt.executeUpdate();
                    }
                }
            }

            conn.commit(); // Transaction Committed Successfully

            response.put("success", true);
            response.put("applicationId", appId);
            response.put("customerId", customerId);
            response.put("status", appStatus);
            response.put("eligibility", eligibility.toMap());
            response.put("message", "Loan application #" + appId + " submitted and persisted successfully in Oracle.");

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("[TX ROLLBACK] Transaction rolled back due to error: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("[ROLLBACK ERROR] " + ex.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }

        return response;
    }

    /**
     * Lists loan applications with optional status filter and customer search.
     */
    public List<LoanApplication> getApplications(String statusFilter, String search) throws SQLException {
        List<LoanApplication> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT la.application_id, la.customer_id, la.loan_type_id, la.requested_amount, la.tenure_months, " +
            "la.calculated_eligible_amount, la.dti_ratio, la.eligibility_outcome, la.outcome_reason, la.application_status, " +
            "la.purpose, TO_CHAR(la.applied_at, 'YYYY-MM-DD HH24:MI:SS') AS applied_at, " +
            "c.full_name AS customer_name, lt.loan_name, lt.interest_rate " +
            "FROM LOAN_APPLICATIONS la " +
            "JOIN CUSTOMERS c ON la.customer_id = c.customer_id " +
            "JOIN LOAN_TYPES lt ON la.loan_type_id = lt.loan_type_id " +
            "WHERE 1=1 "
        );

        if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
            sql.append("AND la.application_status = ? ");
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(c.full_name) LIKE ? OR LOWER(la.purpose) LIKE ? OR LOWER(lt.loan_name) LIKE ?) ");
        }
        sql.append("ORDER BY la.application_id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (statusFilter != null && !statusFilter.trim().isEmpty() && !statusFilter.equalsIgnoreCase("ALL")) {
                stmt.setString(paramIndex++, statusFilter.trim());
            }
            if (search != null && !search.trim().isEmpty()) {
                String term = "%" + search.trim().toLowerCase() + "%";
                stmt.setString(paramIndex++, term);
                stmt.setString(paramIndex++, term);
                stmt.setString(paramIndex++, term);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LoanApplication app = new LoanApplication();
                    app.setApplicationId(rs.getLong("application_id"));
                    app.setCustomerId(rs.getLong("customer_id"));
                    app.setLoanTypeId(rs.getLong("loan_type_id"));
                    app.setRequestedAmount(rs.getDouble("requested_amount"));
                    app.setTenureMonths(rs.getInt("tenure_months"));
                    app.setCalculatedEligibleAmount(rs.getDouble("calculated_eligible_amount"));
                    app.setDtiRatio(rs.getDouble("dti_ratio"));
                    app.setEligibilityOutcome(rs.getString("eligibility_outcome"));
                    app.setOutcomeReason(rs.getString("outcome_reason"));
                    app.setApplicationStatus(rs.getString("application_status"));
                    app.setPurpose(rs.getString("purpose"));
                    app.setAppliedAt(rs.getString("applied_at"));
                    app.setCustomerName(rs.getString("customer_name"));
                    app.setLoanName(rs.getString("loan_name"));
                    app.setInterestRate(rs.getDouble("interest_rate"));
                    list.add(app);
                }
            }
        }
        return list;
    }

    /**
     * Retrieves application details by ID including documents and repayments.
     */
    public Map<String, Object> getApplicationDetails(long applicationId) throws SQLException {
        Map<String, Object> result = new HashMap<>();

        String sqlApp = "SELECT la.application_id, la.customer_id, la.loan_type_id, la.requested_amount, la.tenure_months, " +
                        "la.calculated_eligible_amount, la.dti_ratio, la.eligibility_outcome, la.outcome_reason, la.application_status, " +
                        "la.purpose, TO_CHAR(la.applied_at, 'YYYY-MM-DD HH24:MI:SS') AS applied_at, " +
                        "c.full_name AS customer_name, c.email, c.phone, c.pan_number, c.age, c.address, " +
                        "ed.employment_type, ed.employer_name, ed.monthly_income, ed.experience_years, " +
                        "fd.current_balance, fd.avg_monthly_balance, fd.monthly_turnover, fd.yearly_turnover, fd.existing_monthly_emis, fd.credit_score, " +
                        "lt.loan_name, lt.interest_rate " +
                        "FROM LOAN_APPLICATIONS la " +
                        "JOIN CUSTOMERS c ON la.customer_id = c.customer_id " +
                        "LEFT JOIN EMPLOYMENT_DETAILS ed ON c.customer_id = ed.customer_id " +
                        "LEFT JOIN FINANCIAL_DETAILS fd ON c.customer_id = fd.customer_id " +
                        "JOIN LOAN_TYPES lt ON la.loan_type_id = lt.loan_type_id " +
                        "WHERE la.application_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlApp)) {
            stmt.setLong(1, applicationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> app = new HashMap<>();
                    app.put("applicationId", rs.getLong("application_id"));
                    app.put("customerId", rs.getLong("customer_id"));
                    app.put("loanTypeId", rs.getLong("loan_type_id"));
                    app.put("loanName", rs.getString("loan_name"));
                    app.put("interestRate", rs.getDouble("interest_rate"));
                    app.put("requestedAmount", rs.getDouble("requested_amount"));
                    app.put("tenureMonths", rs.getInt("tenure_months"));
                    app.put("calculatedEligibleAmount", rs.getDouble("calculated_eligible_amount"));
                    app.put("dtiRatio", rs.getDouble("dti_ratio"));
                    app.put("eligibilityOutcome", rs.getString("eligibility_outcome"));
                    app.put("outcomeReason", rs.getString("outcome_reason"));
                    app.put("applicationStatus", rs.getString("application_status"));
                    app.put("purpose", rs.getString("purpose"));
                    app.put("appliedAt", rs.getString("applied_at"));

                    // Customer info
                    app.put("customerName", rs.getString("customer_name"));
                    app.put("email", rs.getString("email"));
                    app.put("phone", rs.getString("phone"));
                    app.put("panNumber", rs.getString("pan_number"));
                    app.put("age", rs.getInt("age"));
                    app.put("address", rs.getString("address"));

                    // Employment & Financial
                    app.put("employmentType", rs.getString("employment_type"));
                    app.put("employerName", rs.getString("employer_name"));
                    app.put("monthlyIncome", rs.getDouble("monthly_income"));
                    app.put("experienceYears", rs.getDouble("experience_years"));
                    app.put("currentBalance", rs.getDouble("current_balance"));
                    app.put("avgMonthlyBalance", rs.getDouble("avg_monthly_balance"));
                    app.put("monthlyTurnover", rs.getDouble("monthly_turnover"));
                    app.put("yearlyTurnover", rs.getDouble("yearly_turnover"));
                    app.put("existingMonthlyEmis", rs.getDouble("existing_monthly_emis"));
                    app.put("creditScore", rs.getInt("credit_score"));

                    result.put("application", app);
                } else {
                    return null;
                }
            }

            // Retrieve Documents
            List<LoanDocument> docs = new ArrayList<>();
            String sqlDocs = "SELECT document_id, application_id, document_name, document_type, verification_status, remarks, " +
                             "TO_CHAR(verified_at, 'YYYY-MM-DD HH24:MI:SS') AS verified_at " +
                             "FROM LOAN_DOCUMENTS WHERE application_id = ? ORDER BY document_id";
            try (PreparedStatement stmtDocs = conn.prepareStatement(sqlDocs)) {
                stmtDocs.setLong(1, applicationId);
                try (ResultSet rsDocs = stmtDocs.executeQuery()) {
                    while (rsDocs.next()) {
                        LoanDocument doc = new LoanDocument();
                        doc.setDocumentId(rsDocs.getLong("document_id"));
                        doc.setApplicationId(rsDocs.getLong("application_id"));
                        doc.setDocumentName(rsDocs.getString("document_name"));
                        doc.setDocumentType(rsDocs.getString("document_type"));
                        doc.setVerificationStatus(rsDocs.getString("verification_status"));
                        doc.setRemarks(rsDocs.getString("remarks"));
                        doc.setVerifiedAt(rsDocs.getString("verified_at"));
                        docs.add(doc);
                    }
                }
            }
            result.put("documents", docs);

            // Retrieve Repayments
            List<LoanRepayment> repayments = new ArrayList<>();
            String sqlRepay = "SELECT repayment_id, application_id, installment_no, TO_CHAR(due_date, 'YYYY-MM-DD') AS due_date, " +
                              "emi_amount, principal_component, interest_component, remaining_balance, payment_status, " +
                              "TO_CHAR(paid_date, 'YYYY-MM-DD') AS paid_date " +
                              "FROM LOAN_REPAYMENTS WHERE application_id = ? ORDER BY installment_no";
            try (PreparedStatement stmtRepay = conn.prepareStatement(sqlRepay)) {
                stmtRepay.setLong(1, applicationId);
                try (ResultSet rsRepay = stmtRepay.executeQuery()) {
                    while (rsRepay.next()) {
                        LoanRepayment lr = new LoanRepayment();
                        lr.setRepaymentId(rsRepay.getLong("repayment_id"));
                        lr.setApplicationId(rsRepay.getLong("application_id"));
                        lr.setInstallmentNo(rsRepay.getInt("installment_no"));
                        lr.setDueDate(rsRepay.getString("due_date"));
                        lr.setEmiAmount(rsRepay.getDouble("emi_amount"));
                        lr.setPrincipalComponent(rsRepay.getDouble("principal_component"));
                        lr.setInterestComponent(rsRepay.getDouble("interest_component"));
                        lr.setRemainingBalance(rsRepay.getDouble("remaining_balance"));
                        lr.setPaymentStatus(rsRepay.getString("payment_status"));
                        lr.setPaidDate(rsRepay.getString("paid_date"));
                        repayments.add(lr);
                    }
                }
            }
            result.put("repayments", repayments);
        }

        return result;
    }

    /**
     * Real database portfolio metrics for dashboard.
     */
    public Map<String, Object> getDashboardStats() throws SQLException {
        Map<String, Object> stats = new HashMap<>();

        String sql = "SELECT " +
                     "  (SELECT COUNT(*) FROM CUSTOMERS) AS total_customers, " +
                     "  (SELECT COUNT(*) FROM LOAN_APPLICATIONS) AS total_applications, " +
                     "  (SELECT COUNT(*) FROM LOAN_APPLICATIONS WHERE application_status = 'APPROVED') AS approved_count, " +
                     "  (SELECT COUNT(*) FROM LOAN_APPLICATIONS WHERE application_status = 'UNDER_REVIEW') AS under_review_count, " +
                     "  (SELECT COUNT(*) FROM LOAN_APPLICATIONS WHERE application_status = 'REJECTED') AS rejected_count, " +
                     "  (SELECT NVL(SUM(requested_amount), 0) FROM LOAN_APPLICATIONS) AS total_requested_volume, " +
                     "  (SELECT NVL(SUM(calculated_eligible_amount), 0) FROM LOAN_APPLICATIONS WHERE application_status = 'APPROVED') AS total_approved_volume, " +
                     "  (SELECT NVL(AVG(dti_ratio), 0) FROM LOAN_APPLICATIONS) AS avg_dti_ratio " +
                     "FROM dual";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                stats.put("totalCustomers", rs.getInt("total_customers"));
                stats.put("totalApplications", rs.getInt("total_applications"));
                stats.put("approvedCount", rs.getInt("approved_count"));
                stats.put("underReviewCount", rs.getInt("under_review_count"));
                stats.put("rejectedCount", rs.getInt("rejected_count"));
                stats.put("totalRequestedVolume", rs.getDouble("total_requested_volume"));
                stats.put("totalApprovedVolume", rs.getDouble("total_approved_volume"));
                stats.put("avgDtiRatio", Math.round(rs.getDouble("avg_dti_ratio") * 10.0) / 10.0);
            }
        }
        return stats;
    }
}
