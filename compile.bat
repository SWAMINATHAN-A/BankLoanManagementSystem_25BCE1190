@echo off
echo ==============================================================
echo Compiling Bank Loan Management System (Java 21 + Oracle JDBC)
echo ==============================================================

if not exist bin mkdir bin

javac -encoding UTF-8 -cp ".;lib/ojdbc6.jar" -d bin src/com/bankloan/config/*.java src/com/bankloan/security/*.java src/com/bankloan/model/*.java src/com/bankloan/db/*.java src/com/bankloan/service/*.java src/com/bankloan/handler/*.java src/com/bankloan/*.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation successful. Bytecode generated in bin/
