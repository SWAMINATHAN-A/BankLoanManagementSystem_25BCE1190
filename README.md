# Bank Loan Management System

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Oracle](https://img.shields.io/badge/Oracle-11g%20XE-F80000?logo=oracle&logoColor=white)](https://www.oracle.com/database/)
[![JDBC](https://img.shields.io/badge/Driver-JDBC%20Thin-007396)](https://www.oracle.com/database/technologies/appdev/jdbc.html)
[![Frontend](https://img.shields.io/badge/Frontend-HTML5%20%7C%20CSS3%20%7C%20JS-E34F26)](https://developer.mozilla.org/)
[![DBMS](https://img.shields.io/badge/Coursework-DBMS%20PAT--4-blue)]()

A lightweight, full-stack bank loan processing application and DBMS demonstration built for the college **DBMS PAT-4** practical. It integrates a native Java HTTP backend with an Oracle 11g Express Edition database to provide role-based authentication, loan application workflows, automated credit eligibility estimation, and interactive SQL demonstrations.

---

## Choose How to Run

### Option 1: Run Locally (Recommended for Professor Evaluation)

1. **Prerequisites:**
   - Java 21 (or 17+) configured in your system `PATH`.
   - Oracle Database 11g Express Edition installed and running (`OracleServiceXE` and `OracleXETNSListener` active).
2. **Configure Database Credentials:**
   - Copy `db.properties.example` to `config/db.properties`.
   - Update with your local Oracle port, username, and password:
     ```properties
     server.port=8085
     db.url=jdbc:oracle:thin:@localhost:1521:xe
     db.username=pat4_user
     db.password=YOUR_LOCAL_PASSWORD
     ```
3. **Initialize Database (Fresh Installations Only):**
   - Execute the schema DDL in [sql/01_schema.sql](sql/01_schema.sql) followed by sample records in [sql/02_sample_data.sql](sql/02_sample_data.sql) in your local Oracle user account.
4. **Compile & Start Server:**
   ```cmd
   .\compile.bat
   .\run.bat
   ```
5. **Access Application:**
   - Open [http://localhost:8085](http://localhost:8085) in your web browser.
   - *Note: `http://localhost:8085` connects to the local server running on that specific computer. It will only open on the machine where the Java backend is actively running.*

**Evaluation Demo Credentials:**
- **Administrator:** `admin` / `admin123`
- **Loan Officer:** `officer1` / `officer123`

---

### Option 2: Live Demo (Temporary)

> **Live URL:** [https://courts-advertisement-crafts-effective.trycloudflare.com](https://courts-advertisement-crafts-effective.trycloudflare.com)

**Demo Credentials:**
- **Administrator:** `admin` / `admin123`
- **Loan Officer:** `officer1` / `officer123`

> [!NOTE]
> *This is a temporary public demo connected to the host laptop. The host laptop, Oracle XE database, Java backend, and Cloudflare Tunnel must remain running for the link to be reachable.*

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Frontend** | HTML5 · CSS3 · Vanilla JavaScript (SPA, Fetch API) |
| **Backend** | Java 21 OpenJDK (`com.sun.net.httpserver.HttpServer` on port `8085`) |
| **Database** | Oracle Database 11g Express Edition (XE) |
| **Connectivity** | Oracle JDBC Thin Driver (`ojdbc6.jar`) · Windows ODBC (`OracleXE_PAT4`) |
| **Tooling & VCS** | Git · GitHub · Batch Scripts (`compile.bat`, `run.bat`, `public_url.bat`) |

---

## Key Features

- **Role-Based Authentication:** Secure login for Administrator and Loan Officer accounts using salted SHA-256 password hashing.
- **Interactive Dashboard:** Real-time metrics showing total applications, volume, and approval breakdowns retrieved directly from Oracle.
- **Loan Application Processing:** Complete workflow to create, view, search, review, and approve or reject applicant files.
- **Eligibility & EMI Calculator:** Automated Debt-to-Income (DTI) ratio evaluation and loan eligibility calculation engine.
- **Live SQL Demonstrations:** Interactive web console to run live DDL (`CREATE`/`ALTER`), DML (`INSERT`/`UPDATE`/`DELETE`), and DQL (multi-table relational joins).
- **Database Connectivity Checks:** Independent status diagnostic buttons for Oracle JDBC connectivity and Windows 64-bit ODBC DSN.

---

## Database

### Relational Tables (8)
`USERS` · `CUSTOMERS` · `EMPLOYMENT_DETAILS` · `FINANCIAL_DETAILS` · `LOAN_TYPES` · `LOAN_APPLICATIONS` · `LOAN_DOCUMENTS` · `LOAN_REPAYMENTS`

### Architecture
```mermaid
flowchart LR
    A["Browser (HTML5 / CSS3 / JS)"] -->|"HTTP / REST API (Port 8085)"| B["Java Backend (HttpServer)"]
    B -->|"Oracle Thin JDBC (ojdbc6.jar)"| C["Oracle XE 11g (Port 1521)"]
```

---

## Repositories

- **Personal Repository:** [https://github.com/arushigrover07/Bank_Loan-Management-System](https://github.com/arushigrover07/Bank_Loan-Management-System)
- **Submission Repository:** [https://github.com/arushigrover07/BankLoanManagementSystem_25BCE1190](https://github.com/arushigrover07/BankLoanManagementSystem_25BCE1190)
