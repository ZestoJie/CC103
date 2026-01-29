@echo off
REM ============================================================
REM   CC103 - SIMPLE DISTRIBUTION CREATOR
REM   Packages everything needed for other computers
REM ============================================================

setlocal enabledelayedexpansion

cd /d "%~dp0"

echo.
echo ============================================================
echo   CC103 - Creating Distribution Package
echo ============================================================
echo.

REM Check if pom.xml exists
if not exist "pom.xml" (
    echo ERROR: pom.xml not found!
    echo Please run this from the cc103 project root directory.
    pause
    exit /b 1
)

REM Create dist folder
echo [1/3] Creating distribution folder...
if exist "dist" rmdir /s /q "dist"
mkdir "dist\cc103-portable"

echo [2/3] Copying application files...
REM Copy source and pom
mkdir "dist\cc103-portable\src"
xcopy "src" "dist\cc103-portable\src" /E /I /Y >nul
copy "pom.xml" "dist\cc103-portable\"
copy "README.md" "dist\cc103-portable\"
copy ".gitignore" "dist\cc103-portable\"

echo [3/3] Creating launcher scripts...

REM Create main launcher for the distribution
(
    echo @echo off
    echo REM ============================================================
    echo REM CC103 - Portable Application Launcher
    echo REM ============================================================
    echo.
    echo setlocal enabledelayedexpansion
    echo cd /d "%%~dp0"
    echo.
    echo echo.
    echo echo ============================================================
    echo echo   CC103 Application Launcher
    echo echo ============================================================
    echo echo.
    echo.
    echo REM Check Maven
    echo mvn -v >nul 2^>^&1
    echo if errorlevel 1 (
    echo     echo ERROR: Maven is required but not found!
    echo     echo.
    echo     echo Please install Maven 3.9.12 or later from:
    echo     echo   https://maven.apache.org/download.cgi
    echo     echo.
    echo     echo Then add Maven to your PATH environment variable.
    echo     echo.
    echo     pause
    echo     exit /b 1
    echo ^)
    echo.
    echo REM Check Java 21
    echo java -version >nul 2^>^&1
    echo if errorlevel 1 (
    echo     echo ERROR: Java 21 is required but not found!
    echo     echo.
    echo     echo Please install Java 21 from:
    echo     echo   https://www.oracle.com/java/technologies/downloads/
    echo     echo.
    echo     pause
    echo     exit /b 1
    echo ^)
    echo.
    echo echo [1/3] Starting Spring Boot Server...
    echo start "CC103 Server" cmd /k "mvn spring-boot:run"
    echo.
    echo echo [2/3] Waiting for server to initialize (20 seconds^)...
    echo timeout /t 20 /nobreak
    echo.
    echo echo [3/3] Launching JavaFX Application...
    echo mvn javafx:run
    echo.
    echo pause
) > "dist\cc103-portable\RUN-ME.bat"

REM Create simplified README for distribution
(
    echo CC103 - Portable Application
    echo ==============================
    echo.
    echo QUICK START
    echo ===========
    echo 1. Install Java 21: https://www.oracle.com/java/technologies/downloads/
    echo 2. Install Maven 3.9.12: https://maven.apache.org/download.cgi
    echo 3. Double-click: RUN-ME.bat
    echo.
    echo REQUIREMENTS
    echo =============
    echo - Windows 7 or later
    echo - Java 21 JDK
    echo - Maven 3.9+
    echo - Internet connection (for first run to download dependencies^)
    echo.
    echo TEST ACCOUNTS (password: password123^)
    echo ========================================
    echo - testuser
    echo - admin
    echo - john
    echo - jane
    echo.
    echo TROUBLESHOOTING
    echo ================
    echo Q: Command not found error?
    echo A: Add Maven and Java to your PATH environment variable
    echo.
    echo Q: Port 8080 already in use?
    echo A: Change the port in src/main/resources/application.properties
    echo.
) > "dist\cc103-portable\START-HERE.txt"

echo.
echo ============================================================
echo   ✅ Distribution package created!
echo.
echo   Location: .\dist\cc103-portable\
echo.
echo   📦 To share with others:
echo      1. Copy the entire 'cc103-portable' folder
echo      2. Recipients need: Java 21 + Maven 3.9+
echo      3. Recipients double-click: RUN-ME.bat
echo.
echo   📝 See START-HERE.txt for instructions
echo ============================================================
echo.

pause
