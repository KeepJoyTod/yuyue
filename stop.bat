@echo off
chcp 65001 > nul
title Amber Film - Stop Services

echo.
echo ========================================
echo    Amber Film - Stop All Services
echo ========================================
echo.

REM Stop backend
echo [INFO] Stopping backend service...
taskkill /FI "WINDOWTITLE eq Amber Film Backend*" /T /F > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Backend service stopped
) else (
    echo [WARN] Backend service window not found
)

REM Stop frontend
echo [INFO] Stopping frontend service...
taskkill /FI "WINDOWTITLE eq Amber Film Frontend*" /T /F > nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Frontend service stopped
) else (
    echo [WARN] Frontend service window not found
)

echo.
echo ========================================
echo    [DONE] Operation completed!
echo ========================================
echo.
pause
