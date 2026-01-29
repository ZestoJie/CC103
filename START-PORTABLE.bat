@echo off
REM ============================================================
REM   CC103 - PORTABLE ONE CLICK STARTUP (JAR BASED)
REM   Works on any Windows computer with Java 21 installed
REM ============================================================

setlocal enabledelayedexpansion

cd /d "%~dp0"

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo ============================================================
    echo   ❌ ERROR: Java 21 is not installed!
    echo ============================================================
    echo.
    echo Please download and install Java 21 from:
    echo https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   CC103 Application - Portable Launcher
echo ============================================================
echo.

REM Check if JARs exist
if not exist "target\cc103-1.0.0-server.jar" (
    echo ❌ Server JAR not found!
    echo Please run: mvn clean package
    pause
    exit /b 1
)

if not exist "target\cc103-app.jar" (
    echo ❌ Client JAR not found!
    echo Please run: mvn clean package
    pause
    exit /b 1
)

echo [1/3] Starting Spring Boot Server...
start "CC103 Server" java -jar "target\cc103-1.0.0-server.jar"

echo [2/3] Waiting for server to initialize (15 seconds)...
timeout /t 15 /nobreak

echo [3/3] Launching JavaFX Application...
java -jar "target\cc103-app.jar"

echo.
echo ============================================================
echo   ✅ Application closed!
echo ============================================================
echo.

pause
