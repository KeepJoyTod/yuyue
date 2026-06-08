@echo off
title Amber Film - Debug Mode
echo [DEBUG] Script started at %date% %time%
echo.

REM Display current directory
echo [DEBUG] Current directory: %cd%
echo.

REM Display script directory
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
echo [DEBUG] Script directory: %SCRIPT_DIR%
echo.

REM List project files
echo [DEBUG] Project files:
dir "%SCRIPT_DIR%" /b
echo.

REM Check Java
echo [DEBUG] Checking Java...
where java
if %errorlevel% neq 0 (
    echo [ERROR] Java not found in PATH
) else (
    java -version
)
echo.

REM Check Node
echo [DEBUG] Checking Node...
where node
if %errorlevel% neq 0 (
    echo [ERROR] Node not found in PATH
) else (
    node -v
    where npm
    if %errorlevel% equ 0 (
        npm -v
    )
)
echo.

REM Check Maven
echo [DEBUG] Checking Maven...
where mvn
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found in PATH
) else (
    mvn -v
)
echo.

REM Check server directory
echo [DEBUG] Checking server directory:
if exist "%SCRIPT_DIR%\server" (
    echo [OK] Server directory exists
    dir "%SCRIPT_DIR%\server" /b
) else (
    echo [ERROR] Server directory not found
)
echo.

REM Check client directory
echo [DEBUG] Checking client directory:
if exist "%SCRIPT_DIR%\client" (
    echo [OK] Client directory exists
    dir "%SCRIPT_DIR%\client" /b
) else (
    echo [ERROR] Client directory not found
)
echo.

REM Check client package.json
if exist "%SCRIPT_DIR%\client\package.json" (
    echo [OK] package.json found
) else (
    echo [ERROR] package.json not found
)
echo.

REM Check client node_modules
if exist "%SCRIPT_DIR%\client\node_modules" (
    echo [OK] node_modules found
) else (
    echo [WARN] node_modules not found - may need to run 'npm install'
)
echo.

echo [DEBUG] Debug info complete!
echo.
echo Press any key to try starting the services...
pause > nul

echo.
echo [INFO] Now starting backend...
start "Amber Film Backend" cmd /k "cd /d %SCRIPT_DIR%\server && mvn spring-boot:run"

echo [INFO] Waiting 5 seconds...
timeout /t 5 > nul

echo [INFO] Now starting frontend...
start "Amber Film Frontend" cmd /k "cd /d %SCRIPT_DIR%\client && npm run dev:h5"

echo.
echo [DONE] Services should be starting in separate windows!
echo.
pause
