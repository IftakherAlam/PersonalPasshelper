# Password Manager (Desktop)

A small, local password manager built with JavaFX. The primary goal is simple and focused: let you save your passwords in this app with proper security.

Passwords are stored locally in an SQLite database and are encrypted with an AES key derived from your master password using PBKDF2 (HMAC-SHA256). The app never stores your master password in plaintext — only a secure PBKDF2 hash and a per-installation salt are stored. This README explains how to build, run and contribute to the project, plus a summary of functionality and troubleshooting tips for GitHub.

---

## Prerequisites

- Java 17 (JDK 17). Confirm with:

```bash
java -version
```

- Maven 3.6+ installed. Confirm with:

```bash
mvn -version
```

- A networked environment for Maven to download dependencies (first run).


## Project Structure (important files)

- `desktop-app/` - The JavaFX desktop application module (primary focus)
  - `src/main/java/com/iftakher/passwordmanager/` - Java sources
  - `src/main/resources/views/` - FXML views (login, main, password dialog)
  - `pom.xml` - Maven module POM for the desktop app
- `android-app/` - Android app (not covered by this README)


## Build and Run (Desktop)

From the project root or specifically inside the `desktop-app` folder.

1) Compile the project (fast check):

```bash
cd desktop-app
mvn -DskipTests=true clean compile
```

2) Run the JavaFX app via the Maven plugin:

```bash
mvn javafx:run
```

This uses the JavaFX Maven plugin configured in the module POM and should launch the GUI.

3) (Optional) Build a runnable JAR (if configured):

```bash
mvn -DskipTests=true package
```

Depending on the POM, packaging may produce a jar under `target/`.


## Creating a Windows .exe Installer

To create a native Windows installer (.exe) with your name "Iftakher" in the setup:

**Prerequisites for building installer:**
- JDK 17 or higher (must include jpackage and jlink tools)
- On Windows: WiX Toolset 3.11+ (for creating .exe installers)
  - Download from: https://github.com/wixtoolset/wix3/releases
  - Add WiX bin folder to your PATH

**Build steps:**

1) On Windows, run the build script:

```cmd
cd desktop-app
build-installer.bat
```

2) On Linux/Mac, run:

```bash
cd desktop-app
./build-installer.sh
```

The script will:
- Build the JAR file with all dependencies
- Create a custom Java runtime image (smaller distribution)
- Package everything into a native installer

**Output location:**
- Windows: `desktop-app/target/installer/PasswordManager-1.0.0.exe`
- Linux: `desktop-app/target/installer/passwordmanager_1.0.0_amd64.deb`
- Mac: `desktop-app/target/installer/PasswordManager-1.0.0.dmg`

**Installer features:**
- Vendor name: "Iftakher" (shown in setup wizard)
- Creates Start Menu shortcuts in "Iftakher" folder
- Adds desktop shortcut
- Standard Windows install/uninstall support
- Includes custom Java runtime (no need for users to install Java)

**Note:** The first build may take several minutes as it creates the optimized runtime image.


## How the app works (user flow)

1. Login screen:
   - Enter the master password. If no master password exists yet (first run), the app will accept the entered password and store a derived secure hash.
   - The master password is used to derive an AES key for encrypting/decrypting stored passwords.

2. Main (vault) screen:
   - Table of saved entries showing Title, Username, Password (hidden by default), Website and Category.
   - Password column includes an "eye" button to reveal/copy the plaintext password (decrypted on demand using master password derived key).
   - Buttons: Add Password, Edit, Delete, Export, Import.

3. Add/Edit Password dialog:
   - Enter title, username, website, password, category and notes.
   - Password is encrypted using AES and stored in the local SQLite DB.

4. Export/Import:
   - Export creates an encrypted export file protected by the same encryption key.
   - Import reads that file and attempts to decrypt entries using the master password key.


## Security & Encryption notes

- Master password hashing / migration:
  - The app supports a migration path from older plaintext/legacy storage into PBKDF2-based storage.
  - A salt is generated/stored under `app_settings` in the SQLite DB and used to derive the AES key.

- Encryption:
  - Uses PBKDF2 (HMAC SHA-256) to derive keys and AES/CBC/PKCS5 padding for symmetric encryption.
  - Encryption code is centralized in `EncryptionService`.
  - The app includes Bouncy Castle provider via Maven for crypto compatibility.

- Storage:
  - SQLite DB file `passwords.db` is created in the working directory by the app.
  - Table `passwords` stores encrypted password bytes and IV per row.


## Developer notes and important code locations

- Main GUI controller: `desktop-app/src/main/java/com/iftakher/passwordmanager/controllers/MainController.java`
- Password dialog controller: `desktop-app/src/main/java/com/iftakher/passwordmanager/controllers/PasswordDialogController.java`
- Encryption helpers: `desktop-app/src/main/java/com/iftakher/passwordmanager/services/EncryptionService.java`
- Database helpers: `desktop-app/src/main/java/com/iftakher/passwordmanager/services/DatabaseService.java`
- Password table cell (eye button): `desktop-app/src/main/java/com/iftakher/passwordmanager/controllers/PasswordTableCell.java`
- FXML views: `desktop-app/src/main/resources/views/` (login.fxml, main.fxml, password-dialog.fxml)
- Styles: `desktop-app/src/main/resources/styles/main.css`


## Troubleshooting

- "Invalid master password":
  - Ensure you're typing the right master password. If this is the first run and `master_password_hash` is empty in DB, the app will store the password as the initial master.

- "No installed provider supports this key: (null)" or crypto provider errors:
  - Ensure Maven dependencies include Bouncy Castle (the POM contains `org.bouncycastle:bcprov-jdk15on`).
  - If you run into provider/internal exceptions, try a full `mvn clean package` to ensure dependencies are present.

- Password column shows placeholder but eye button doesn't reveal:
  - The eye relies on the runtime `currentEncryptionKey` which is derived from the supplied master password during login. Verify the key is being set in `MainController.setEncryptionKey(...)`.

- DB migration: if you've got an old DB with plaintext master password pre-existing, the app has code to migrate it when you login with the legacy password.

### Database migration (how to run it now)

If you have an existing database with a legacy plaintext master password (or an older hash format) you can migrate it safely. The app will automatically migrate the master password to the PBKDF2-backed scheme when you successfully login with the existing master password. Here are explicit commands and steps to run locally.

1) Backup your DB (always do this first):

```bash
cd desktop-app
cp passwords.db passwords.db.bak
```

2) Start the app and login (recommended / automatic migration):

```bash
mvn javafx:run
# In the login screen enter your current (legacy) master password.
# If the app accepts it, it will migrate the stored master password to the PBKDF2 scheme and keep your entries encrypted with the derived key.
```

3) Verify the settings in the DB using sqlite3:

```bash
# show master password hash and salt
sqlite3 passwords.db "SELECT key, value FROM app_settings WHERE key IN ('master_password_hash','master_password_salt');"
```

4) (Optional) If you want the app to prompt for a new master password instead of migrating, you can clear the stored hash — the app will treat the next login as first-time setup. WARNING: only do this if you know the current master password or have a DB backup.

```bash
sqlite3 passwords.db "UPDATE app_settings SET value = '' WHERE key = 'master_password_hash';"
# then run the app and set a new master password when prompted
mvn javafx:run
```

Notes:
- The recommended approach is to let the app perform the migration automatically by logging in with the current password; that ensures the in-memory encryption key is available to re-encrypt or validate entries.
- If you need a programmatic (all-in-one) migration tool, I can add a small CLI class that reads the DB, derives the new key and updates the stored settings and entries — tell me if you want a dedicated migration utility.


## Contributing & Development

- Use the `desktop-app` module for local development. Changes to UI controllers and FXML are hot-reloadable when re-running `mvn javafx:run`.
- Keep cryptographic changes explicit and well tested. Avoid changing crypto primitives without a migration path.


## How to test locally

- Start the app with `mvn javafx:run` and create a master password when prompted.
- Add a few password entries, then try the eye toggle in the `Password` column to reveal a password. Try editing and deleting.
- Export and import to verify portability. Ensure you use the same master password to decrypt imports.


## License

Add your project license here (e.g., MIT). This repository doesn't include a license file by default.


