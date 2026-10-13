# Bank Loan Management System — DBMS PAT-4 Practical

A lightweight, student-friendly full-stack web application for bank loan management, credit eligibility estimation, and DBMS demonstration built for the college DBMS PAT-4 practical.

- **Frontend**: HTML5, CSS3, Vanilla JavaScript.
- **Backend**: Java OpenJDK (`com.sun.net.httpserver.HttpServer` on port `8085`).
- **Database**: Oracle Database 11g Express Edition (XE).
- **Connectivity**: Oracle JDBC Driver (`ojdbc6.jar`) and Windows 64-bit ODBC (`OracleXE_PAT4`).

---

## 1. Technologies & Prerequisites

1. **Java JDK 17+ or 21 LTS** installed and available in Windows PATH.
2. **Oracle Database 11g Express Edition (XE)** installed on Windows.
3. **Oracle JDBC Driver**: Included in the project under `lib/ojdbc6.jar`.
4. **Windows 64-bit ODBC DSN**: Configured as `OracleXE_PAT4` using the `Oracle in XE` driver.

---

## 2. Verify Oracle Services & Java

Open PowerShell and verify:

```powershell
# 1. Check Java version
java -version
javac -version

# 2. Verify Oracle XE Windows Services are running
Get-Service -Name *Oracle* | Format-Table -AutoSize
```
Ensure `OracleServiceXE` and `OracleXETNSListener` both report **Running**.

---

## 3. Database Credentials Configuration

Your local credentials are kept securely in `config/db.properties` (never committed to git):

1. Check or create `config/db.properties`:
   ```powershell
   Copy-Item db.properties.example config\db.properties
   ```
2. Verify that `config/db.properties` contains your Oracle username and password:
   ```properties
   server.port=8085
   db.driver=oracle.jdbc.OracleDriver
   db.url=jdbc:oracle:thin:@localhost:1521:xe
   db.username=pat4_user
   db.password=Pat4User123

   odbc.dsn=OracleXE_PAT4
   odbc.driver=Oracle in XE
   odbc.username=pat4_user
   odbc.password=Pat4User123
   ```

---

## 4. Database Setup (Fresh Installations Only)

> **Important**: If your database is already populated, **do NOT re-run these scripts**, as they would attempt to re-create existing tables.

If setting up on a completely fresh Oracle instance:

```cmd
# Run Schema DDL (Creates the 8 tables and sequences)
sqlplus pat4_user/Pat4User123@localhost:1521/xe @sql/01_schema.sql

# Seed Initial Sample Data
sqlplus pat4_user/Pat4User123@localhost:1521/xe @sql/02_sample_data.sql
```

### How to verify all 8 tables and sample records exist:
```cmd
sqlplus pat4_user/Pat4User123@localhost:1521/xe
```
Then run:
```sql
SELECT table_name FROM user_tables ORDER BY table_name;

SELECT 'CUSTOMERS' AS tbl, COUNT(*) AS cnt FROM CUSTOMERS
UNION ALL SELECT 'LOAN_APPLICATIONS', COUNT(*) FROM LOAN_APPLICATIONS
UNION ALL SELECT 'LOAN_TYPES', COUNT(*) FROM LOAN_TYPES;
```

---

## 5. How to Compile and Run the Application

From `BankLoanManagementSystem`:

```cmd
# Step 1: Compile Java source code into bin/
.\compile.bat

# Step 2: Start the Java HTTP server
.\run.bat
```

Open your browser to:
👉 **`http://localhost:8085`**

---

## 6. Demonstration Login Credentials

- **Username:** `admin`
- **Password:** `admin123`

*(On the login screen, click **"Fill Demo Credentials"** to populate automatically).*

---

## 7. How to Test Database Connectivity

### A. Testing JDBC (Java to Oracle)
- Go to the **"Database Connectivity"** tab in the web menu.
- Click **"Test JDBC Connection"**.
- The backend connects directly to Oracle 11g XE using `ojdbc6.jar` and reports database product, version, connected username (`PAT4_USER`), table count, and response latency (~22ms).

### B. Testing ODBC Separately (Windows to Oracle)
- On the **"Database Connectivity"** tab, click **"Test ODBC Connection Separately"**.
- This executes `sql/test_odbc_connection.ps1` using native Windows `System.Data.Odbc` against DSN `OracleXE_PAT4` and displays the genuine connection result.

---

## 8. College Practical Demo Flow

1. **Sign In**: Login using `admin` / `admin123`.
2. **Dashboard**: View the simple 4-box summary of applications and recent records retrieved from Oracle.
3. **Apply for Loan**:
   - Enter your own applicant details, income, banking balances, and requested loan amount.
   - Click **"Check Eligibility Only"** to test the Java eligibility service.
   - Click **"Submit & Save to Oracle Database"** to execute a multi-table database transaction.
4. **View Applications**: See your newly submitted application appear in the searchable table.
5. **SQL Queries (DBMS)**:
   - **DDL**: Click `CREATE TABLE` and `ALTER TABLE` to demonstrate Data Definition Language.
   - **DML**: Click `INSERT`, `UPDATE`, and `DELETE` to demonstrate Data Manipulation Language.
   - **DQL**: Click `3-Table Relational JOIN` and `GROUP BY & Aggregation` to demonstrate relational queries.
6. **Database Connectivity**: Open the dedicated tab and demonstrate both independent JDBC and ODBC test buttons.
