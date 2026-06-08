@echo off
chcp 65001 > nul
title Amber Film - Project Startup

echo.
echo ========================================
echo    Amber Film - Quick Start
echo ========================================
echo.

REM Get script directory
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
echo [INFO] Working from: %SCRIPT_DIR%
echo.

REM Check Java environment
echo [1/3] Checking Java environment...
java -version > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17 or later.
    echo.
    pause
    exit /b 1
)
echo [OK] Java environment ready
echo.

REM Check Node environment
echo [2/3] Checking Node environment...
node -v > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js not found. Please install Node.js.
    echo.
    pause
    exit /b 1
)
for /f "tokens=*" %%i in ('node -v') do set NODE_VERSION=%%i
echo [OK] Node version: %NODE_VERSION%
echo.

REM Check Maven environment
echo [3/3] Checking Maven environment...
call mvn -v > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found. Please install Maven.
    echo.
    pause
    exit /b 1
)
echo [OK] Maven environment ready
echo.

echo ========================================
echo    Starting services...
echo ========================================
echo.

REM Start backend
echo [INFO] Starting backend service (port 8080)...
cd /d "%SCRIPT_DIR%\server"
if errorlevel 1 (
    echo [ERROR] Cannot find server directory: %SCRIPT_DIR%\server
    pause
    exit /b 1
)
start "Amber Film Backend" /D "%SCRIPT_DIR%\server" cmd /k "call mvn spring-boot:run"

REM Wait for backend to start
echo [INFO] Waiting for backend to initialize...
ping -n 6 127.0.0.1 > nul

REM Start frontend
echo [INFO] Starting frontend service (port 10086)...
cd /d "%SCRIPT_DIR%\client"
if errorlevel 1 (
    echo [ERROR] Cannot find client directory: %SCRIPT_DIR%\client
    pause
    exit /b 1
)
start "Amber Film Frontend" /D "%SCRIPT_DIR%\client" cmd /k "call npm run dev:h5"

echo.
echo ========================================
echo    [SUCCESS] All services started!
echo ========================================
echo.
echo Backend: http://localhost:8080
echo Frontend: http://localhost:10086
echo H2 Console: http://localhost:8080/h2-console
echo.
echo [TIP] Close service windows to stop
echo.
echo [INFO] This window will close in 10 seconds...
ping -n 11 127.0.0.1 > nul
exit /b 0
