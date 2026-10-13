# ==============================================================================
# Bank Loan Management System - Genuine ODBC Diagnostic Script
# DBMS PAT-4 Practical | Target: Windows 64-bit ODBC with Oracle XE
# File: sql/test_odbc_connection.ps1
# ==============================================================================
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\sql\test_odbc_connection.ps1
#   or specify parameters:
#   powershell -ExecutionPolicy Bypass -File .\sql\test_odbc_connection.ps1 -DsnName "OracleXE_PAT4" -Username "SYSTEM"
# ==============================================================================

param(
    [string]$DsnName = "OracleXE_PAT4",
    [string]$Username = "",
    [string]$Password = ""
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  Bank Loan Management System - Genuine ODBC Verification " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Timestamp : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "Platform  : $([System.Environment]::OSVersion.VersionString) ($([System.IntPtr]::Size * 8)-bit)"

# Step 1: Check Windows ODBC Data Source availability
Write-Host "`n[Step 1] Checking Windows ODBC Data Source Name (DSN)..." -ForegroundColor Yellow
$dsn = Get-OdbcDsn -Name $DsnName -ErrorAction SilentlyContinue

if (-not $dsn) {
    Write-Host "[FAIL] ODBC DSN '$DsnName' not found on this system." -ForegroundColor Red
    Write-Host "Available ODBC DSNs on this machine:" -ForegroundColor DarkYellow
    Get-OdbcDsn | Format-Table -AutoSize Name, DsnType, Platform, DriverName
    exit 1
}

Write-Host "[OK] DSN Found: $($dsn.Name) [Platform: $($dsn.Platform), Driver: $($dsn.DriverName)]" -ForegroundColor Green

# Step 2: Try to read local config if credentials not passed
if (-not $Username) {
    $configFile = Join-Path $PSScriptRoot "..\config\db.properties"
    if (Test-Path $configFile) {
        Get-Content $configFile | ForEach-Object {
            if ($_ -match "^\s*db\.username\s*=\s*(.+)$") { $Username = $matches[1].Trim() }
            if ($_ -match "^\s*db\.password\s*=\s*(.+)$") { $Password = $matches[1].Trim() }
        }
    }
}

if (-not $Username -or $Username -eq "YOUR_ORACLE_USERNAME") {
    Write-Host "`n[NOTICE] Valid Oracle credentials not supplied or placeholder detected." -ForegroundColor DarkYellow
    Write-Host "DSN '$DsnName' is properly registered in Windows."
    Write-Host "To test active authentication via ODBC, pass credentials:"
    Write-Host "  .\sql\test_odbc_connection.ps1 -DsnName '$DsnName' -Username '<YOUR_USER>' -Password '<YOUR_PWD>'"
    exit 0
}

# Step 3: Attempt real System.Data.Odbc Connection
Write-Host "`n[Step 2] Opening genuine System.Data.Odbc connection to '$DsnName'..." -ForegroundColor Yellow
$connStr = "DSN=$DsnName;Uid=$Username;Pwd=$Password;"

try {
    $conn = New-Object System.Data.Odbc.OdbcConnection($connStr)
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    $conn.Open()
    $stopwatch.Stop()

    Write-Host "[SUCCESS] Genuine ODBC Connection Established!" -ForegroundColor Green
    Write-Host "  Connection Time : $($stopwatch.ElapsedMilliseconds) ms"
    Write-Host "  Server Version  : $($conn.ServerVersion)"
    Write-Host "  Driver Name     : $($conn.Driver)"

    # Step 4: Run a real query against dual or schema
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = "SELECT sys_context('USERENV', 'CURRENT_USER') AS curr_user, sys_context('USERENV', 'DB_NAME') AS db_name FROM dual"
    $reader = $cmd.ExecuteReader()
    if ($reader.Read()) {
        Write-Host "  Connected User  : $($reader['curr_user'])"
        Write-Host "  Database Name   : $($reader['db_name'])"
    }
    $reader.Close()
    $conn.Close()
    Write-Host "`n[ODBC TEST STATUS: PASSED]" -ForegroundColor Green
}
catch {
    Write-Host "[FAIL] ODBC Connection attempt failed:" -ForegroundColor Red
    Write-Host "  Error Message: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
