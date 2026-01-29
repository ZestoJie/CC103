@echo off
REM ============================================================
REM   CC103 - ONE CLICK STARTUP
REM   Runs Spring Boot Server + JavaFX App Automatically
REM ============================================================

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo.
echo ============================================================
echo   CC103 Application Startup
echo ============================================================
echo.

REM Start Spring Boot Server in background
echo [1/3] Starting Spring Boot Server on port 8080...
start "CC103 Server" cmd /k ""%cd%\run-server.bat""

REM Wait for server to initialize
echo [2/3] Waiting for server to initialize (15 seconds)...
timeout /t 15 /nobreak

REM Start JavaFX App
echo [3/3] Launching JavaFX Login Application...
start "CC103 App" cmd /k ""%cd%\run-app.bat""

echo.
echo ============================================================
echo   ✅ Both applications are starting!
echo.
echo   📱 JavaFX Login Window: Should appear shortly
echo   🌐 Server: http://localhost:8080
echo.
echo   📝 Test Credentials (all have password: password123):
echo      - testuser
echo      - admin
echo      - john
echo      - jane
echo ============================================================
echo.

timeout /t 5
