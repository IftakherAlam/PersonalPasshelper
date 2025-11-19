#!/bin/bash
# Build script for creating installers (Linux/Mac)
# Author: Iftakher

echo "Building Password Manager installer..."
echo ""

# Step 1: Clean and package
echo "Step 1: Building JAR file..."
mvn clean package -DskipTests=true
if [ $? -ne 0 ]; then
    echo "Error: Maven build failed"
    exit 1
fi

# Step 2: Create runtime image with jlink
echo ""
echo "Step 2: Creating Java runtime image..."
jlink --add-modules java.base,java.sql,java.desktop,java.logging,java.naming,java.xml,jdk.crypto.ec \
      --output target/java-runtime \
      --strip-debug \
      --no-header-files \
      --no-man-pages \
      --compress=2

if [ $? -ne 0 ]; then
    echo "Error: jlink failed"
    exit 1
fi

# Step 3: Detect OS and create appropriate installer
echo ""
echo "Step 3: Creating installer..."

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    INSTALLER_TYPE="deb"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    INSTALLER_TYPE="dmg"
else
    INSTALLER_TYPE="app-image"
fi

jpackage --input target \
         --name "PasswordManager" \
         --main-jar password-manager-desktop-1.0.0.jar \
         --main-class com.iftakher.passwordmanager.Main \
         --runtime-image target/java-runtime \
         --dest target/installer \
         --type "$INSTALLER_TYPE" \
         --vendor "Iftakher" \
         --app-version "1.0.0" \
         --description "A secure local password manager" \
         --copyright "Copyright (C) 2025 Iftakher"

if [ $? -ne 0 ]; then
    echo "Error: jpackage failed"
    exit 1
fi

echo ""
echo "========================================"
echo "Build completed successfully!"
echo "Installer location: target/installer/"
echo "========================================"
