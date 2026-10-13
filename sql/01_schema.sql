-- ==============================================================================
-- Bank Loan Management System - Database Schema (DDL)
-- DBMS PAT-4 Practical | Target: Oracle 11g XE
-- File: sql/01_schema.sql
-- ==============================================================================
-- SAFETY NOTICE:
-- 1. This script DOES NOT automatically DROP existing tables or truncate data.
-- 2. Tables and sequences are created in strict dependency order.
-- 3. If any table already exists, creating it again will raise ORA-00955 (name
--    is already used by an existing object).
-- 4. To inspect existing objects prior to execution:
--      SELECT table_name FROM user_tables ORDER BY table_name;
-- 5. If you intentionally wish to reset a prior installation for testing,
--    manually drop child tables first before parent tables in reverse order:
--      DROP TABLE LOAN_REPAYMENTS CASCADE CONSTRAINTS;
--      DROP TABLE LOAN_DOCUMENTS CASCADE CONSTRAINTS;
--      DROP TABLE LOAN_APPLICATIONS CASCADE CONSTRAINTS;
--      DROP TABLE FINANCIAL_DETAILS CASCADE CONSTRAINTS;
--      DROP TABLE EMPLOYMENT_DETAILS CASCADE CONSTRAINTS;
--      DROP TABLE CUSTOMERS CASCADE CONSTRAINTS;
--      DROP TABLE LOAN_TYPES CASCADE CONSTRAINTS;
--      DROP TABLE USERS CASCADE CONSTRAINTS;
-- ==============================================================================

SET ECHO ON;
SET FEEDBACK ON;

-- ------------------------------------------------------------------------------
-- 1. SEQUENCES (for primary keys)
-- ------------------------------------------------------------------------------
CREATE SEQUENCE seq_users START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_customers START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_employment START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_financial START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_loan_types START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_applications START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_documents START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_repayments START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ------------------------------------------------------------------------------
-- 2. TABLE 1: USERS
-- Manages application access, roles, and salted password hashes.
-- ------------------------------------------------------------------------------
CREATE TABLE USERS (
    user_id         NUMBER(10)          NOT NULL,
    username        VARCHAR2(50)        NOT NULL,
    password_hash   VARCHAR2(128)       NOT NULL,
    salt            VARCHAR2(64)        NOT NULL,
    full_name       VARCHAR2(100)       NOT NULL,
    role            VARCHAR2(20)        DEFAULT 'OFFICER' NOT NULL,
    status          VARCHAR2(20)        DEFAULT 'ACTIVE' NOT NULL,
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'OFFICER', 'APPLICANT')),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

-- ------------------------------------------------------------------------------
-- 3. TABLE 2: CUSTOMERS
-- Master record of loan applicants with demographic & contact information.
-- Optional 1-to-1 link to USERS if the customer possesses self-service login.
-- ------------------------------------------------------------------------------
CREATE TABLE CUSTOMERS (
    customer_id     NUMBER(10)          NOT NULL,
    user_id         NUMBER(10),
    full_name       VARCHAR2(100)       NOT NULL,
    email           VARCHAR2(100)       NOT NULL,
    phone           VARCHAR2(20)        NOT NULL,
    age             NUMBER(3)           NOT NULL,
    gender          VARCHAR2(10)        NOT NULL,
    pan_number      VARCHAR2(10)        NOT NULL,
    address         VARCHAR2(250)       NOT NULL,
    created_at      TIMESTAMP           DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT uk_customers_pan UNIQUE (pan_number),
    CONSTRAINT fk_customers_user FOREIGN KEY (user_id) REFERENCES USERS (user_id),
    CONSTRAINT chk_customers_age CHECK (age >= 18 AND age <= 80),
    CONSTRAINT chk_customers_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- ------------------------------------------------------------------------------
-- 4. TABLE 3: EMPLOYMENT_DETAILS
-- Professional background, income stability, and organization info.
-- 1-to-1 relationship with CUSTOMERS.
-- ------------------------------------------------------------------------------
CREATE TABLE EMPLOYMENT_DETAILS (
    employment_id           NUMBER(10)      NOT NULL,
    customer_id             NUMBER(10)      NOT NULL,
    employment_type         VARCHAR2(30)    NOT NULL,
    employer_name           VARCHAR2(150)   NOT NULL,
    designation             VARCHAR2(100)   NOT NULL,
    monthly_income          NUMBER(12, 2)   NOT NULL,
    experience_years        NUMBER(4, 1)    NOT NULL,
    office_address          VARCHAR2(250),
    CONSTRAINT pk_employment PRIMARY KEY (employment_id),
    CONSTRAINT uk_employment_customer UNIQUE (customer_id),
    CONSTRAINT fk_employment_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT chk_emp_type CHECK (employment_type IN ('SALARIED', 'SELF_EMPLOYED', 'BUSINESS', 'PROFESSIONAL')),
    CONSTRAINT chk_emp_income CHECK (monthly_income >= 0),
    CONSTRAINT chk_emp_exp CHECK (experience_years >= 0)
);

-- ------------------------------------------------------------------------------
-- 5. TABLE 4: FINANCIAL_DETAILS
-- Applicant banking liquidity, cash flow turnover, and existing EMI burden.
-- 1-to-1 relationship with CUSTOMERS.
-- ------------------------------------------------------------------------------
CREATE TABLE FINANCIAL_DETAILS (
    financial_id            NUMBER(10)      NOT NULL,
    customer_id             NUMBER(10)      NOT NULL,
    current_balance         NUMBER(12, 2)   DEFAULT 0 NOT NULL,
    avg_monthly_balance     NUMBER(12, 2)   DEFAULT 0 NOT NULL,
    monthly_turnover        NUMBER(14, 2)   DEFAULT 0 NOT NULL,
    yearly_turnover         NUMBER(14, 2)   DEFAULT 0 NOT NULL,
    existing_monthly_emis   NUMBER(12, 2)   DEFAULT 0 NOT NULL,
    credit_score            NUMBER(4)       DEFAULT 700 NOT NULL,
    updated_at              TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT pk_financial PRIMARY KEY (financial_id),
    CONSTRAINT uk_financial_customer UNIQUE (customer_id),
    CONSTRAINT fk_financial_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT chk_fin_curr_bal CHECK (current_balance >= 0),
    CONSTRAINT chk_fin_avg_bal CHECK (avg_monthly_balance >= 0),
    CONSTRAINT chk_fin_m_turnover CHECK (monthly_turnover >= 0),
    CONSTRAINT chk_fin_y_turnover CHECK (yearly_turnover >= 0),
    CONSTRAINT chk_fin_emis CHECK (existing_monthly_emis >= 0),
    CONSTRAINT chk_fin_cscore CHECK (credit_score >= 300 AND credit_score <= 900)
);

-- ------------------------------------------------------------------------------
-- 6. TABLE 5: LOAN_TYPES
-- Configurable loan products, interest benchmarks, and eligibility thresholds.
-- ------------------------------------------------------------------------------
CREATE TABLE LOAN_TYPES (
    loan_type_id            NUMBER(10)      NOT NULL,
    loan_name               VARCHAR2(50)    NOT NULL,
    description             VARCHAR2(250),
    interest_rate           NUMBER(5, 2)    NOT NULL,
    min_income_required     NUMBER(12, 2)   NOT NULL,
    max_loan_limit          NUMBER(14, 2)   NOT NULL,
    max_tenure_months       NUMBER(4)       NOT NULL,
    dti_max_threshold       NUMBER(5, 2)    DEFAULT 50.0 NOT NULL,
    is_active               VARCHAR2(1)     DEFAULT 'Y' NOT NULL,
    CONSTRAINT pk_loan_types PRIMARY KEY (loan_type_id),
    CONSTRAINT uk_loan_types_name UNIQUE (loan_name),
    CONSTRAINT chk_lt_interest CHECK (interest_rate > 0 AND interest_rate <= 40),
    CONSTRAINT chk_lt_min_income CHECK (min_income_required >= 0),
    CONSTRAINT chk_lt_max_loan CHECK (max_loan_limit > 0),
    CONSTRAINT chk_lt_max_tenure CHECK (max_tenure_months > 0),
    CONSTRAINT chk_lt_dti CHECK (dti_max_threshold > 0 AND dti_max_threshold <= 100),
    CONSTRAINT chk_lt_active CHECK (is_active IN ('Y', 'N'))
);

-- ------------------------------------------------------------------------------
-- 7. TABLE 6: LOAN_APPLICATIONS
-- Core transaction records storing request params, eligibility output, and status.
-- ------------------------------------------------------------------------------
CREATE TABLE LOAN_APPLICATIONS (
    application_id              NUMBER(10)      NOT NULL,
    customer_id                 NUMBER(10)      NOT NULL,
    loan_type_id                NUMBER(10)      NOT NULL,
    requested_amount            NUMBER(14, 2)   NOT NULL,
    tenure_months               NUMBER(4)       NOT NULL,
    calculated_eligible_amount  NUMBER(14, 2)   DEFAULT 0 NOT NULL,
    dti_ratio                   NUMBER(5, 2)    DEFAULT 0 NOT NULL,
    eligibility_outcome         VARCHAR2(30)    NOT NULL,
    outcome_reason              VARCHAR2(400),
    application_status          VARCHAR2(30)    DEFAULT 'SUBMITTED' NOT NULL,
    purpose                     VARCHAR2(200)   NOT NULL,
    applied_at                  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reviewed_at                 TIMESTAMP,
    CONSTRAINT pk_applications PRIMARY KEY (application_id),
    CONSTRAINT fk_app_customer FOREIGN KEY (customer_id) REFERENCES CUSTOMERS (customer_id),
    CONSTRAINT fk_app_loan_type FOREIGN KEY (loan_type_id) REFERENCES LOAN_TYPES (loan_type_id),
    CONSTRAINT chk_app_req_amt CHECK (requested_amount > 0),
    CONSTRAINT chk_app_tenure CHECK (tenure_months > 0),
    CONSTRAINT chk_app_elig_outcome CHECK (eligibility_outcome IN ('ELIGIBLE', 'CONDITIONAL', 'INSUFFICIENT_INCOME', 'HIGH_DTI', 'INELIGIBLE')),
    CONSTRAINT chk_app_status CHECK (application_status IN ('SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'DISBURSED', 'CLOSED'))
);

-- ------------------------------------------------------------------------------
-- 8. TABLE 7: LOAN_DOCUMENTS
-- Applicant verification documents and compliance audit logs.
-- ------------------------------------------------------------------------------
CREATE TABLE LOAN_DOCUMENTS (
    document_id             NUMBER(10)      NOT NULL,
    application_id          NUMBER(10)      NOT NULL,
    document_name           VARCHAR2(100)   NOT NULL,
    document_type           VARCHAR2(50)    NOT NULL,
    verification_status     VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    remarks                 VARCHAR2(200),
    verified_at             TIMESTAMP,
    CONSTRAINT pk_documents PRIMARY KEY (document_id),
    CONSTRAINT fk_doc_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATIONS (application_id),
    CONSTRAINT chk_doc_status CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED'))
);

-- ------------------------------------------------------------------------------
-- 9. TABLE 8: LOAN_REPAYMENTS
-- Amortized repayment schedules and payment tracking for demonstration loans.
-- ------------------------------------------------------------------------------
CREATE TABLE LOAN_REPAYMENTS (
    repayment_id            NUMBER(10)      NOT NULL,
    application_id          NUMBER(10)      NOT NULL,
    installment_no          NUMBER(4)       NOT NULL,
    due_date                DATE            NOT NULL,
    emi_amount              NUMBER(12, 2)   NOT NULL,
    principal_component     NUMBER(12, 2)   NOT NULL,
    interest_component      NUMBER(12, 2)   NOT NULL,
    remaining_balance       NUMBER(14, 2)   NOT NULL,
    payment_status          VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    paid_date               DATE,
    CONSTRAINT pk_repayments PRIMARY KEY (repayment_id),
    CONSTRAINT uk_repayment_inst UNIQUE (application_id, installment_no),
    CONSTRAINT fk_repay_application FOREIGN KEY (application_id) REFERENCES LOAN_APPLICATIONS (application_id),
    CONSTRAINT chk_repay_status CHECK (payment_status IN ('PENDING', 'PAID', 'OVERDUE')),
    CONSTRAINT chk_repay_emi CHECK (emi_amount >= 0)
);

COMMIT;
PROMPT Schema creation completed successfully.
