@echo off
setlocal
REM ============================================================================
REM  Production startup script (ICh-Platform backend)
REM  Forces the 'mysql' Spring profile so a deployment that forgets the profile
REM  cannot silently boot the H2 in-memory demo database with the dev JWT secret.
REM
REM  Usage:
REM    start-prod.cmd
REM  Prerequisites:
REM    1) schema + seed SQL imported (see sql/00_schema.sql and 10/11 seed files)
REM    2) Env vars ICH_DB_USER / ICH_DB_PASSWORD / ICH_JWT_SECRET are set
REM    3) Backend built, or Maven available for spring-boot:run
REM  Note: ICH_JWT_SECRET must be a random string of at least 32 bytes
REM        (e.g. generated with `openssl rand -base64 48`). Never commit real
REM        credentials into the repository.
REM ============================================================================

cd /d "%~dp0backend"

REM ---- 1. Require production env vars; refuse to start if any is missing ----
set "MISSING="
if "%ICH_DB_USER%"=="" set "MISSING=%MISSING% ICH_DB_USER"
if "%ICH_DB_PASSWORD%"=="" set "MISSING=%MISSING% ICH_DB_PASSWORD"
if "%ICH_JWT_SECRET%"=="" set "MISSING=%MISSING% ICH_JWT_SECRET"

if not "%MISSING%"=="" (
    echo [ERROR] Missing required production environment variables:%MISSING%
    echo         Set them first. PowerShell example:
    echo           $env:ICH_DB_USER = "root"
    echo           $env:ICH_DB_PASSWORD = "your-db-password"
    echo           $env:ICH_JWT_SECRET = "random-secret-at-least-32-bytes"
    echo         See README.md section "Production (MySQL) mode" for details.
    exit /b 1
)

REM ---- 2. Force the mysql profile ----
set "ICH_SPRING_PROFILE=mysql"

REM ---- 3. Choose Maven: prefer bundled tools/ mvn, else PATH mvn ----
REM      (do NOT use `if exist` here: cmd cannot resolve Chinese paths reliably)
set "MAVEN_CMD=%~dp0tools\apache-maven-3.9.9\bin\mvn.cmd"
where mvn >nul 2>&1
if %errorlevel%==0 set "MAVEN_CMD=mvn"

echo [INFO] Starting backend with mysql profile (port 8080)...
echo [INFO] If a jar is already built, use: java -jar target\ich-platform.jar --spring.profiles.active=mysql
call "%MAVEN_CMD%" spring-boot:run
exit /b %errorlevel%
