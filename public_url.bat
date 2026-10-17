@echo off
setlocal enabledelayedexpansion

echo ==================================================================
echo   Bank Loan Management System — Temporary Cloudflare HTTPS Tunnel
echo ==================================================================
echo.

:: 1. Verify that the local Java backend is running on 127.0.0.1:8085
echo [1/3] Checking if local backend server is running on http://127.0.0.1:8085...
powershell -NoProfile -Command "try { $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8085/api/health' -UseBasicParsing -TimeoutSec 3; if ($r.StatusCode -eq 200) { exit 0 } else { exit 1 } } catch { exit 1 }"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] The Java backend server is NOT running on http://127.0.0.1:8085!
    echo.
    echo Please start the backend server in a separate terminal before running this script:
    echo   1. Open a new Command Prompt
    echo   2. Run: .\run.bat
    echo   3. Once you see "[SERVER READY]", re-run this script: .\public_url.bat
    echo.
    pause
    exit /b 1
)
echo [OK] Java backend is active and responding on http://127.0.0.1:8085.
echo.

:: 2. Locate cloudflared binary (check PATH first, then local tools\cloudflared.exe)
echo [2/3] Checking for Cloudflare Tunnel executable (cloudflared)...
set CLOUDFLARED_BIN=
where cloudflared >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set CLOUDFLARED_BIN=cloudflared
) else if exist "tools\cloudflared.exe" (
    set CLOUDFLARED_BIN=tools\cloudflared.exe
)

if "%CLOUDFLARED_BIN%"=="" (
    echo [ERROR] cloudflared.exe was not found on your system PATH or in the 'tools\' directory.
    echo.
    echo To use this script safely without installing anything globally:
    echo   1. Download the official signed Windows binary from Cloudflare's GitHub releases:
    echo      https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe
    echo.
    echo   2. Create a 'tools' folder in this project and place the downloaded file there as:
    echo      tools\cloudflared.exe
    echo.
    echo   (Note: 'tools/' and '*.exe' are already ignored in .gitignore so it will never be committed)
    echo.
    pause
    exit /b 1
)

echo [OK] Found Cloudflare Tunnel executable: %CLOUDFLARED_BIN%
echo.

:: 3. Informational security warning
echo [3/3] Preparing public HTTPS tunnel for http://127.0.0.1:8085...
echo ==================================================================
echo   SECURITY NOTICE:
echo   - Anyone with this temporary public URL can access the website
echo     and test login / loan applications.
echo   - Oracle port 1521 is NOT exposed. Only port 8085 is tunneled.
echo   - Keep this terminal window open while your professor evaluates.
echo   - Press CTRL+C at any time to immediately TERMINATE the tunnel.
echo ==================================================================
echo.
echo Starting tunnel... Look for the public HTTPS URL below:
echo.

"%CLOUDFLARED_BIN%" tunnel --url http://127.0.0.1:8085

echo.
echo [TUNNEL TERMINATED] The public URL is no longer active.
pause
