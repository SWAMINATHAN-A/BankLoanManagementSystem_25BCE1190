-- ==============================================================================
-- Bank Loan Management System - Demonstration & Seed Data (DML)
-- DBMS PAT-4 Practical | Target: Oracle 11g XE
-- File: sql/02_sample_data.sql
-- ==============================================================================
-- SAFETY NOTICE:
-- 1. This script uses MERGE / conditional INSERT logic where appropriate to prevent
--    duplicate record errors when rerun.
-- 2. Fixed identifier blocks (101-105, etc.) are used for demonstration records.
-- 3. Initial Administrator Setup:
--    The initial administrator account below is seeded with a placeholder hash.
--    In Batch 2, a CLI setup utility (com.bankloan.security.PasswordUtil) or
--    the initial setup wizard will generate the real salt + SHA-256 hash.
-- ==============================================================================

SET ECHO ON;
SET FEEDBACK ON;

-- ------------------------------------------------------------------------------
-- 1. SEED: USERS (System Staff & Administrator)
-- ------------------------------------------------------------------------------
-- Admin Account (User ID 1):
-- Placeholder salt/hash provided for initial DDL/DML validation.
-- A standalone Java command in Batch 2 safely provisions your custom password.
MERGE INTO USERS u
USING (
    SELECT 1 AS user_id, 'admin' AS username, 
           'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' AS password_hash,
           'd41d8cd98f00b204e9800998ecf8427e' AS salt,
           'System Administrator' AS full_name, 'ADMIN' AS role, 'ACTIVE' AS status FROM dual
) src
ON (u.user_id = src.user_id OR u.username = src.username)
WHEN NOT MATCHED THEN
    INSERT (user_id, username, password_hash, salt, full_name, role, status)
    VALUES (src.user_id, src.username, src.password_hash, src.salt, src.full_name, src.role, src.status);

-- Loan Officer Account (User ID 2):
MERGE INTO USERS u
USING (
    SELECT 2 AS user_id, 'officer1' AS username,
           'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' AS password_hash,
           'd41d8cd98f00b204e9800998ecf8427e' AS salt,
           'Senior Credit Officer Rajesh' AS full_name, 'OFFICER' AS role, 'ACTIVE' AS status FROM dual
) src
ON (u.user_id = src.user_id OR u.username = src.username)
WHEN NOT MATCHED THEN
    INSERT (user_id, username, password_hash, salt, full_name, role, status)
    VALUES (src.user_id, src.username, src.password_hash, src.salt, src.full_name, src.role, src.status);

-- ------------------------------------------------------------------------------
-- 2. SEED: LOAN_TYPES (Standard Banking Credit Products)
-- ------------------------------------------------------------------------------
MERGE INTO LOAN_TYPES lt
USING (
    SELECT 1 AS loan_type_id, 'Home Loan' AS loan_name, 
           'Financing for purchasing or constructing residential property' AS description,
           8.50 AS interest_rate, 35000.00 AS min_income_required, 
           10000000.00 AS max_loan_limit, 360 AS max_tenure_months, 55.0 AS dti_max_threshold, 'Y' AS is_active FROM dual
    UNION ALL
    SELECT 2, 'Personal Loan', 
           'Unsecured financing for immediate personal or emergency expenses',
           11.75, 25000.00, 1500000.00, 60, 45.0, 'Y' FROM dual
    UNION ALL
    SELECT 3, 'Business Expansion Loan', 
           'Working capital and capital expenditure loan for micro and SME enterprises',
           12.50, 60000.00, 5000000.00, 84, 50.0, 'Y' FROM dual
    UNION ALL
    SELECT 4, 'Vehicle Loan', 
           'Secured auto loan for new four-wheelers and commercial vehicles',
           9.25, 20000.00, 2500000.00, 84, 50.0, 'Y' FROM dual
    UNION ALL
    SELECT 5, 'Education Loan', 
           'Funding for higher educational courses in premier domestic and global institutions',
           9.00, 20000.00, 4000000.00, 120, 50.0, 'Y' FROM dual
) src
ON (lt.loan_type_id = src.loan_type_id)
WHEN NOT MATCHED THEN
    INSERT (loan_type_id, loan_name, description, interest_rate, min_income_required, max_loan_limit, max_tenure_months, dti_max_threshold, is_active)
    VALUES (src.loan_type_id, src.loan_name, src.description, src.interest_rate, src.min_income_required, src.max_loan_limit, src.max_tenure_months, src.dti_max_threshold, src.is_active);

-- ------------------------------------------------------------------------------
-- 3. SEED: CUSTOMERS (Realistic Demonstration Profiles)
-- ------------------------------------------------------------------------------
MERGE INTO CUSTOMERS c
USING (
    SELECT 101 AS customer_id, NULL AS user_id, 'Aarav Sharma' AS full_name,
           'aarav.sharma@example.com' AS email, '9876543210' AS phone, 32 AS age, 'MALE' AS gender,
           'ABCPS1234A' AS pan_number, 'Flat 402, Royal Residency, Connaught Place, New Delhi' AS address FROM dual
    UNION ALL
    SELECT 102, NULL, 'Priya Nair',
           'priya.nair@example.com', '9812345678', 29, 'FEMALE',
           'BNPNA5678B', 'Villa 12, Palm Meadows, Whitefield, Bengaluru' FROM dual
    UNION ALL
    SELECT 103, NULL, 'Vikramaditya Rao',
           'vikram.rao@example.com', '9723456789', 44, 'MALE',
           'CKPRA9012C', 'Plot 88, Jubilee Hills, Road No 36, Hyderabad' FROM dual
    UNION ALL
    SELECT 104, NULL, 'Ananya Sen',
           'ananya.sen@example.com', '9634567890', 26, 'FEMALE',
           'DPSEN3456D', '14/B, Southern Avenue, Ballygunge, Kolkata' FROM dual
    UNION ALL
    SELECT 105, NULL, 'Rohit Kulkarni',
           'rohit.k@example.com', '9545678901', 38, 'MALE',
           'EKPKU7890E', 'Flat 701, Hill View Heights, Baner, Pune' FROM dual
) src
ON (c.customer_id = src.customer_id)
WHEN NOT MATCHED THEN
    INSERT (customer_id, user_id, full_name, email, phone, age, gender, pan_number, address)
    VALUES (src.customer_id, src.user_id, src.full_name, src.email, src.phone, src.age, src.gender, src.pan_number, src.address);

-- ------------------------------------------------------------------------------
-- 4. SEED: EMPLOYMENT_DETAILS (1-to-1 with CUSTOMERS)
-- ------------------------------------------------------------------------------
MERGE INTO EMPLOYMENT_DETAILS ed
USING (
    SELECT 201 AS employment_id, 101 AS customer_id, 'SALARIED' AS employment_type,
           'Tata Consultancy Services' AS employer_name, 'Lead Solution Architect' AS designation,
           135000.00 AS monthly_income, 9.5 AS experience_years, 'Barakhamba Road, New Delhi' AS office_address FROM dual
    UNION ALL
    SELECT 202, 102, 'SALARIED',
           'Infosys Limited', 'Senior Systems Analyst',
           88000.00, 6.0, 'Electronics City Phase 1, Bengaluru' FROM dual
    UNION ALL
    SELECT 203, 103, 'SELF_EMPLOYED',
           'Rao Precision Engineering Works', 'Managing Director',
           240000.00, 16.0, 'Sanath Nagar Industrial Estate, Hyderabad' FROM dual
    UNION ALL
    SELECT 204, 104, 'SALARIED',
           'PwC India', 'Tax Consultant',
           42000.00, 2.5, 'Salt Lake Sector V, Kolkata' FROM dual
    UNION ALL
    SELECT 205, 105, 'BUSINESS',
           'Kulkarni Agro Logistics', 'Proprietor',
           165000.00, 11.0, 'Marketyard, Gultekdi, Pune' FROM dual
) src
ON (ed.customer_id = src.customer_id)
WHEN NOT MATCHED THEN
    INSERT (employment_id, customer_id, employment_type, employer_name, designation, monthly_income, experience_years, office_address)
    VALUES (src.employment_id, src.customer_id, src.employment_type, src.employer_name, src.designation, src.monthly_income, src.experience_years, src.office_address);

-- ------------------------------------------------------------------------------
-- 5. SEED: FINANCIAL_DETAILS (1-to-1 with CUSTOMERS)
-- ------------------------------------------------------------------------------
MERGE INTO FINANCIAL_DETAILS fd
USING (
    SELECT 301 AS financial_id, 101 AS customer_id, 
           485000.00 AS current_balance, 390000.00 AS avg_monthly_balance,
           145000.00 AS monthly_turnover, 1740000.00 AS yearly_turnover,
           18500.00 AS existing_monthly_emis, 785 AS credit_score FROM dual
    UNION ALL
    SELECT 302, 102,
           215000.00, 180000.00,
           92000.00, 1104000.00,
           9500.00, 760 FROM dual
    UNION ALL
    SELECT 303, 103,
           1850000.00, 1420000.00,
           680000.00, 8160000.00,
           45000.00, 810 FROM dual
    UNION ALL
    SELECT 304, 104,
           64000.00, 48000.00,
           44000.00, 528000.00,
           14000.00, 680 FROM dual
    UNION ALL
    SELECT 305, 105,
           520000.00, 410000.00,
           380000.00, 4560000.00,
           28000.00, 740 FROM dual
) src
ON (fd.customer_id = src.customer_id)
WHEN NOT MATCHED THEN
    INSERT (financial_id, customer_id, current_balance, avg_monthly_balance, monthly_turnover, yearly_turnover, existing_monthly_emis, credit_score)
    VALUES (src.financial_id, src.customer_id, src.current_balance, src.avg_monthly_balance, src.monthly_turnover, src.yearly_turnover, src.existing_monthly_emis, src.credit_score);

-- ------------------------------------------------------------------------------
-- 6. SEED: LOAN_APPLICATIONS
-- ------------------------------------------------------------------------------
MERGE INTO LOAN_APPLICATIONS la
USING (
    -- Customer 101 (Aarav): Home loan, well within eligibility -> APPROVED
    SELECT 401 AS application_id, 101 AS customer_id, 1 AS loan_type_id,
           4500000.00 AS requested_amount, 240 AS tenure_months,
           5800000.00 AS calculated_eligible_amount, 36.20 AS dti_ratio,
           'ELIGIBLE' AS eligibility_outcome,
           'Strong disposable income and clean credit profile with low DTI.' AS outcome_reason,
           'APPROVED' AS application_status,
           'Purchase of ready-to-move apartment in Dwarka' AS purpose FROM dual
    UNION ALL
    -- Customer 102 (Priya): Vehicle loan -> UNDER_REVIEW
    SELECT 402, 102, 4,
           1200000.00, 60,
           1450000.00, 31.80,
           'ELIGIBLE',
           'Comfortable cash balance and steady salaried cash flows.',
           'UNDER_REVIEW',
           'Purchase of electric SUV' FROM dual
    UNION ALL
    -- Customer 103 (Vikramaditya): Business expansion loan -> APPROVED
    SELECT 403, 103, 3,
           3000000.00, 48,
           3850000.00, 35.40,
           'ELIGIBLE',
           'High business turnover and excellent credit standing.',
           'APPROVED',
           'Procurement of 5-axis CNC industrial milling machinery' FROM dual
    UNION ALL
    -- Customer 104 (Ananya): Personal loan -> REJECTED due to high DTI
    SELECT 404, 104, 2,
           800000.00, 36,
           350000.00, 61.90,
           'HIGH_DTI',
           'Existing debt obligations consume greater than 60% of monthly income.',
           'REJECTED',
           'International holiday and relocation expenses' FROM dual
    UNION ALL
    -- Customer 105 (Rohit): Business loan -> SUBMITTED
    SELECT 405, 105, 3,
           2000000.00, 60,
           2400000.00, 38.50,
           'ELIGIBLE',
           'Good turnover and steady logistics operating balance.',
           'SUBMITTED',
           'Fleet enhancement with 2 refrigerated transport carriers' FROM dual
) src
ON (la.application_id = src.application_id)
WHEN NOT MATCHED THEN
    INSERT (application_id, customer_id, loan_type_id, requested_amount, tenure_months, calculated_eligible_amount, dti_ratio, eligibility_outcome, outcome_reason, application_status, purpose)
    VALUES (src.application_id, src.customer_id, src.loan_type_id, src.requested_amount, src.tenure_months, src.calculated_eligible_amount, src.dti_ratio, src.eligibility_outcome, src.outcome_reason, src.application_status, src.purpose);

-- ------------------------------------------------------------------------------
-- 7. SEED: LOAN_DOCUMENTS (Compliance verification checks)
-- ------------------------------------------------------------------------------
MERGE INTO LOAN_DOCUMENTS ld
USING (
    SELECT 501 AS document_id, 401 AS application_id, 'PAN Card Copy' AS document_name, 'IDENTITY_PROOF' AS document_type, 'VERIFIED' AS verification_status, 'Verified against NSDL database' AS remarks FROM dual
    UNION ALL
    SELECT 502, 401, 'Salary Slips (Last 3 Months)', 'INCOME_PROOF', 'VERIFIED', 'Matches employer net credit' FROM dual
    UNION ALL
    SELECT 503, 401, 'Bank Statement (6 Months)', 'BANK_STATEMENT', 'VERIFIED', 'No inward cheque returns' FROM dual
    UNION ALL
    SELECT 504, 401, 'Property Allotment Letter', 'COLLATERAL_PROOF', 'VERIFIED', 'Title search clear' FROM dual
    UNION ALL
    SELECT 505, 402, 'Aadhaar Card Copy', 'IDENTITY_PROOF', 'VERIFIED', 'UIDAI biometric validated' FROM dual
    UNION ALL
    SELECT 506, 402, 'Form 16 / ITR V', 'INCOME_PROOF', 'PENDING', 'Awaiting verified portal copy' FROM dual
    UNION ALL
    SELECT 507, 403, 'GST Registration Certificate', 'BUSINESS_PROOF', 'VERIFIED', 'Active status confirmed' FROM dual
    UNION ALL
    SELECT 508, 403, 'Audited Balance Sheet (2 Yrs)', 'FINANCIAL_REPORT', 'VERIFIED', 'Certified by chartered accountant' FROM dual
    UNION ALL
    SELECT 509, 404, 'Salary Slip', 'INCOME_PROOF', 'VERIFIED', 'Verified against corporate HR' FROM dual
    UNION ALL
    SELECT 510, 405, 'Udyam MSME Certificate', 'BUSINESS_PROOF', 'PENDING', 'Under verification with registry' FROM dual
) src
ON (ld.document_id = src.document_id)
WHEN NOT MATCHED THEN
    INSERT (document_id, application_id, document_name, document_type, verification_status, remarks)
    VALUES (src.document_id, src.application_id, src.document_name, src.document_type, src.verification_status, src.remarks);

-- ------------------------------------------------------------------------------
-- 8. SEED: LOAN_REPAYMENTS (Installment schedules for demonstration loans)
-- Sample amortized installments for Approved Home Loan (Application 401)
-- ------------------------------------------------------------------------------
MERGE INTO LOAN_REPAYMENTS lr
USING (
    SELECT 601 AS repayment_id, 401 AS application_id, 1 AS installment_no,
           TO_DATE('2026-04-05', 'YYYY-MM-DD') AS due_date,
           39052.28 AS emi_amount, 7177.28 AS principal_component, 31875.00 AS interest_component,
           4492822.72 AS remaining_balance, 'PAID' AS payment_status, TO_DATE('2026-04-04', 'YYYY-MM-DD') AS paid_date FROM dual
    UNION ALL
    SELECT 602, 401, 2,
           TO_DATE('2026-05-05', 'YYYY-MM-DD'),
           39052.28, 7228.12, 31824.16,
           4485594.60, 'PAID', TO_DATE('2026-05-05', 'YYYY-MM-DD') FROM dual
    UNION ALL
    SELECT 603, 401, 3,
           TO_DATE('2026-06-05', 'YYYY-MM-DD'),
           39052.28, 7279.32, 31772.96,
           4478315.28, 'PAID', TO_DATE('2026-06-03', 'YYYY-MM-DD') FROM dual
    UNION ALL
    SELECT 604, 401, 4,
           TO_DATE('2026-07-05', 'YYYY-MM-DD'),
           39052.28, 7330.88, 31721.40,
           4470984.40, 'PENDING', NULL FROM dual
    UNION ALL
    SELECT 605, 401, 5,
           TO_DATE('2026-08-05', 'YYYY-MM-DD'),
           39052.28, 7382.83, 31669.45,
           4463601.57, 'PENDING', NULL FROM dual
) src
ON (lr.repayment_id = src.repayment_id)
WHEN NOT MATCHED THEN
    INSERT (repayment_id, application_id, installment_no, due_date, emi_amount, principal_component, interest_component, remaining_balance, payment_status, paid_date)
    VALUES (src.repayment_id, src.application_id, src.installment_no, src.due_date, src.emi_amount, src.principal_component, src.interest_component, src.remaining_balance, src.payment_status, src.paid_date);

COMMIT;
PROMPT Sample data seeded successfully without duplicates.
