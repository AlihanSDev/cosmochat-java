@echo off
REM CosmoChat Direct Launcher for Windows
REM This bypasses PowerShell encoding issues by directly invoking Maven and Java

setlocal enabledelayedexpansion

REM Change to script directory
cd /d "%~dp0"

echo ============================================================
echo   CosmoChat Build & Startup
echo ============================================================
echo.

REM ---------------------------------
REM 1. Build Java project
REM ---------------------------------
echo [1/4] Building Java application...
if exist "..\apache-maven-3.9.9\bin\mvn.cmd" (
    set MAVEN=..\apache-maven-3.9.9\bin\mvn.cmd
) else if exist "..\apache-maven\bin\mvn.cmd" (
    set MAVEN=..\apache-maven\bin\mvn.cmd
) else (
    echo ERROR: Maven not found in ../apache-maven-3.9.9/bin or ../apache-maven/bin
    pause
    exit /b 1
)

echo Using Maven: %MAVEN%
call %MAVEN% clean compile -DskipTests
if errorlevel 1 (
    echo.
    echo BUILD FAILED
    pause
    exit /b 1
)
echo Build complete.
echo.

REM ---------------------------------
REM 2. Database configuration
REM ---------------------------------
echo [2/4] Configuring database connection...
set DB_URL=jdbc:postgresql://127.0.0.1:5433/cosmochat
set DB_USER=cosmochat
set DB_PASSWORD=Brown1234_

REM Show configuration (masked)
echo   DB URL: %DB_URL%
echo   DB User: %DB_USER%
echo   DB Pass: ***
echo.

REM ---------------------------------
REM 3. Prepare Java Module Path
REM ---------------------------------
echo [3/4] Preparing Java module path...
set USERPROFILE=%USERPROFILE%
set CP=target\classes;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.11\javafx-base-21.0.11-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.11\javafx-controls-21.0.11-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.11\javafx-graphics-21.0.11-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-web\21.0.11\javafx-web-21.0.11-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-media\21.0.11\javafx-media-21.0.11-win.jar;%USERPROFILE%\.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar

REM Verify existence of critical jars (optional)
if not exist "%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.4\postgresql-42.7.4.jar" (
    echo WARNING: PostgreSQL JDBC driver not found. Maven should have downloaded it.
)
echo.

REM ---------------------------------
REM 4. Initialize Database (DbInit)
REM ---------------------------------
echo [4/4] Initializing database schema...
java -Dcosmochat.db.url=%DB_URL% -Dcosmochat.db.user=%DB_USER% -Dcosmochat.db.password=%DB_PASSWORD% --module-path "%CP%" -m cosmochat/cosmochat.database.DbInit
if errorlevel 1 (
    echo.
    echo DATABASE INITIALIZATION FAILED
    pause
    exit /b 1
)
echo Database OK.
echo.

REM ---------------------------------
REM 5. Start Python Backend (optional)
REM ---------------------------------
echo [5/5] Starting Python AI backend...
REM Try to start the Python backend in a new minimized window
start "" /MIN python backend\coder_api.py
timeout /t 8 /nobreak >nul
echo.

REM ---------------------------------
REM 6. Launch JavaFX Application
REM ---------------------------------
echo Launching CosmoChat UI...
java -Dcosmochat.db.url=%DB_URL% -Dcosmochat.db.user=%DB_USER% -Dcosmochat.db.password=%DB_PASSWORD% --module-path "%CP%" -m cosmochat/cosmochat.CosmoChatApp
if errorlevel 1 (
    echo.
    echo APPLICATION CRASHED
    pause
    exit /b 1
)

endlocal
