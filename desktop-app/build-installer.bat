@echo off
REM Build script for creating Windows .exe installer
REM Author: Iftakher

echo Building Password Manager installer...
echo.

REM Check if Java is available
java -version >nul 2>&1
if errorlevel 1 (
    echo Error: Java is not installed or not in PATH
    echo Please install JDK 17+ and try again
    pause
    exit /b 1
)

REM Step 1: Clean and package
echo Step 1: Building JAR file...
call mvn clean package -DskipTests=true
if errorlevel 1 (
    echo Error: Maven build failed
    pause
    exit /b 1
)

REM Step 2: Verify JAR was created
if not exist "target\password-manager-desktop-1.0.0.jar" (
    echo Error: JAR file was not created
    pause
    exit /b 1
)

REM Step 3: Clean old installer
if exist "target\installer" (
    echo Cleaning old installer directory...
    rmdir /S /Q target\installer
)

REM Step 4: Create installer with jpackage (with bundled JRE)
echo.
echo Step 2: Creating Windows installer with bundled Java runtime...
echo This may take a few minutes...

REM Check if icon exists
set ICON_PARAM=
if exist "src\main\resources\icons\app-icon.ico" (
    echo Using custom icon...
    set ICON_PARAM=--icon src\main\resources\icons\app-icon.ico
)

jpackage --input target --name PersonalPasshelper --main-jar password-manager-desktop-1.0.0.jar --main-class com.iftakher.passwordmanager.Launcher --dest target\installer --type exe --vendor Iftakher --app-version 1.0.0 --description "A secure local password manager" --copyright "Copyright (C) 2025 Iftakher" --win-dir-chooser --win-menu --win-shortcut --win-menu-group PersonalPasshelper --java-options "-Dfile.encoding=UTF-8" %ICON_PARAM%

if errorlevel 1 (
    echo Error: jpackage failed
    echo Make sure you have a full JDK not just JRE installed
    pause
    exit /b 1
)

REM Verify installer was created
if not exist target\installer\PersonalPasshelper-1.0.0.exe (
    echo Error: Installer was not created
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo Installer location: target\installer\PersonalPasshelper-1.0.0.exe
echo Installer size: 
dir target\installer\PersonalPasshelper-1.0.0.exe | findstr PersonalPasshelper
echo ========================================
echo.
echo You can now run the installer to install the application.
echo.
pause
