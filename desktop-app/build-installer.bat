@echo off
REM Build script for creating Windows .exe installer
REM Author: Iftakher

echo Building Password Manager installer...
echo.

REM Step 1: Clean and package
echo Step 1: Building JAR file...
call mvn clean package -DskipTests=true
if errorlevel 1 (
    echo Error: Maven build failed
    exit /b 1
)

REM Step 2: Create runtime image with jlink
echo.
echo Step 2: Creating Java runtime image...
jlink --add-modules java.base,java.sql,java.desktop,java.logging,java.naming,java.xml,jdk.crypto.ec ^
      --output target/java-runtime ^
      --strip-debug ^
      --no-header-files ^
      --no-man-pages ^
      --compress=2

if errorlevel 1 (
    echo Error: jlink failed
    exit /b 1
)

REM Step 3: Create installer with jpackage
echo.
echo Step 3: Creating Windows installer...
jpackage --input target ^
         --name "PasswordManager" ^
         --main-jar password-manager-desktop-1.0.0.jar ^
         --main-class com.iftakher.passwordmanager.Main ^
         --runtime-image target/java-runtime ^
         --dest target/installer ^
         --type exe ^
         --vendor "Iftakher" ^
         --app-version "1.0.0" ^
         --description "A secure local password manager" ^
         --copyright "Copyright (C) 2025 Iftakher" ^
         --win-dir-chooser ^
         --win-menu ^
         --win-shortcut ^
         --win-menu-group "Iftakher"

if errorlevel 1 (
    echo Error: jpackage failed
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo Installer location: target\installer\PasswordManager-1.0.0.exe
echo ========================================
pause
