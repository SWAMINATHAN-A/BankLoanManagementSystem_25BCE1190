-- ==============================================================================
-- Bank Loan Management System - SQL Demonstration Queries (DDL / DML / DQL)
-- DBMS PAT-4 Practical | Target: Oracle 11g XE
-- File: sql/03_demo_queries.sql
-- ==============================================================================
-- PURPOSE:
-- Demonstrates real DDL, DML, and DQL operations matching the 8-table schema.
-- Includes safety explanations, constraints, and relational queries.
-- ==============================================================================

SET PAGESIZE 50;
SET LINESIZE 200;
SET FEEDBACK ON;

PROMPT =========================================================================;
PROMPT SECTION 1: DATA DEFINITION LANGUAGE (DDL) DEMONSTRATION;
PROMPT =========================================================================;

-- DDL DEMO 1: CREATE TABLE
-- Creates a dedicated audit log table demonstrating constraints, timestamps,
-- and Oracle numeric/character types.
-- NOTE: Safe to run once during demonstration.
CREATE TABLE LOAN_AUDIT_LOG (
    log_id          NUMBER(10)          NOT NULL,
    action_type     VARCHAR2(30)        NOT NULL,
    entity_name     VARCHAR2(50)        NOT NULL,
    record_id       NUMBER(10)          NOT NULL,
    performed_by    VARCHAR2(50)        NOT NULL,
    log_timestamp   TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    details         VARCHAR2(400),
    CONSTRAINT pk_loan_audit PRIMARY KEY (log_id),
    CONSTRAINT chk_audit_action CHECK (action_type IN ('INSERT', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'LOGIN'))
);

-- DDL DEMO 2: ALTER TABLE
-- Adds an optional column to an existing table demonstrating table modification.
ALTER TABLE CUSTOMERS ADD (alternate_phone VARCHAR2(20));

-- Verification of DDL alterations
DESC LOAN_AUDIT_LOG;
DESC CUSTOMERS;


PROMPT =========================================================================;
PROMPT SECTION 2: DATA MANIPULATION LANGUAGE (DML) DEMONSTRATION;
PROMPT =========================================================================;

-- DML DEMO 1: INSERT a demonstration customer record
-- Demonstrates parameterized insertion into CUSTOMERS with safe disposable ID 999.
INSERT INTO CUSTOMERS (
    customer_id, user_id, full_name, email, phone, age, gender, pan_number, address
) VALUES (
    999, NULL, 'Kavita Verma (Demo Applicant)', 'kavita.demo@example.com',
    '9998887776', 31, 'FEMALE', 'ZZZKV9999Z', '101 Demonstration Enclave, Sector 14, Gurugram'
);

-- Accompanying financial record for demo applicant
INSERT INTO FINANCIAL_DETAILS (
    financial_id, customer_id, current_balance, avg_monthly_balance, 
    monthly_turnover, yearly_turnover, existing_monthly_emis, credit_score
) VALUES (
    999, 999, 120000.00, 95000.00, 75000.00, 900000.00, 5000.00, 720
);

-- DML DEMO 2: UPDATE an existing record
-- Demonstrates updating loan status and outcome reason with prepared parameters.
UPDATE LOAN_APPLICATIONS
SET application_status = 'UNDER_REVIEW',
    outcome_reason = 'Credit officer requested additional collateral appraisal documents.'
WHERE application_id = 405;

-- DML DEMO 3: SAFE DELETE of a disposable test record
-- SAFEGUARD EXPLANATION:
-- We only delete the disposable test applicant (ID 999) created during demonstration.
-- Child foreign-key records (FINANCIAL_DETAILS) are deleted first to respect
-- referential integrity constraints, followed by the parent record.
-- Existing real/seed records (101-105) are never touched by this delete statement.
DELETE FROM FINANCIAL_DETAILS WHERE customer_id = 999;
DELETE FROM CUSTOMERS WHERE customer_id = 999;

COMMIT;


PROMPT =========================================================================;
PROMPT SECTION 3: DATA QUERY LANGUAGE (DQL) DEMONSTRATION;
PROMPT =========================================================================;

-- DQL 1: Multi-table JOIN (Customer Profile, Employment, and Financial Health)
-- Combines 3 tables to review applicant creditworthiness in a single report.
SELECT 
    c.customer_id,
    c.full_name,
    c.age,
    ed.employment_type,
    ed.employer_name,
    ed.monthly_income,
    fd.avg_monthly_balance,
    fd.existing_monthly_emis,
    fd.credit_score
FROM CUSTOMERS c
JOIN EMPLOYMENT_DETAILS ed ON c.customer_id = ed.customer_id
JOIN FINANCIAL_DETAILS fd  ON c.customer_id = fd.customer_id
ORDER BY c.customer_id;

-- DQL 2: Comprehensive Loan Application Register with Loan Product Details
SELECT 
    la.application_id,
    c.full_name AS applicant_name,
    lt.loan_name,
    lt.interest_rate AS rate_pct,
    la.requested_amount,
    la.calculated_eligible_amount,
    la.dti_ratio,
    la.eligibility_outcome,
    la.application_status
FROM LOAN_APPLICATIONS la
JOIN CUSTOMERS c   ON la.customer_id = c.customer_id
JOIN LOAN_TYPES lt ON la.loan_type_id = lt.loan_type_id
ORDER BY la.application_id;

-- DQL 3: Filter Applications by Status (Filtering by 'APPROVED' or 'UNDER_REVIEW')
SELECT 
    la.application_id,
    c.full_name,
    la.requested_amount,
    la.tenure_months,
    la.application_status,
    la.purpose
FROM LOAN_APPLICATIONS la
JOIN CUSTOMERS c ON la.customer_id = c.customer_id
WHERE la.application_status IN ('APPROVED', 'UNDER_REVIEW')
ORDER BY la.requested_amount DESC;

-- DQL 4: Aggregation and Group Summary (Portfolio Exposure by Loan Product)
SELECT 
    lt.loan_name,
    COUNT(la.application_id) AS total_applications,
    NVL(SUM(la.requested_amount), 0) AS total_amount_requested,
    NVL(AVG(la.calculated_eligible_amount), 0) AS avg_eligible_amount,
    NVL(MIN(la.requested_amount), 0) AS min_requested,
    NVL(MAX(la.requested_amount), 0) AS max_requested
FROM LOAN_TYPES lt
LEFT JOIN LOAN_APPLICATIONS la ON lt.loan_type_id = la.loan_type_id
GROUP BY lt.loan_name
ORDER BY total_applications DESC;

-- DQL 5: Document Compliance Audit Status per Loan Application
SELECT 
    la.application_id,
    c.full_name AS applicant_name,
    COUNT(ld.document_id) AS total_documents_required,
    SUM(CASE WHEN ld.verification_status = 'VERIFIED' THEN 1 ELSE 0 END) AS verified_count,
    SUM(CASE WHEN ld.verification_status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count
FROM LOAN_APPLICATIONS la
JOIN CUSTOMERS c ON la.customer_id = c.customer_id
LEFT JOIN LOAN_DOCUMENTS ld ON la.application_id = ld.application_id
GROUP BY la.application_id, c.full_name
ORDER BY la.application_id;

-- DQL 6: Repayment Ledger Status and Outstanding Amortization
SELECT 
    lr.repayment_id,
    la.application_id,
    c.full_name AS borrower_name,
    lr.installment_no,
    TO_CHAR(lr.due_date, 'YYYY-MM-DD') AS due_date,
    lr.emi_amount,
    lr.principal_component,
    lr.interest_component,
    lr.remaining_balance,
    lr.payment_status
FROM LOAN_REPAYMENTS lr
JOIN LOAN_APPLICATIONS la ON lr.application_id = la.application_id
JOIN CUSTOMERS c ON la.customer_id = c.customer_id
ORDER BY lr.application_id, lr.installment_no;
