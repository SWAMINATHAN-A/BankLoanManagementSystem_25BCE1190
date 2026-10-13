@echo off
echo ==============================================================
echo Starting Bank Loan Management System Server on port 8085
echo ==============================================================

java -cp ".;bin;lib/ojdbc6.jar" com.bankloan.Main
