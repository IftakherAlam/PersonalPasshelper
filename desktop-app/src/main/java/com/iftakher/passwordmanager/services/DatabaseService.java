package com.iftakher.passwordmanager.services;

import com.iftakher.passwordmanager.models.PasswordEntry;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.crypto.SecretKey;

public class DatabaseService {
    private static final String DB_URL = "jdbc:sqlite:passwords.db";
    private Connection connection;
    private byte[] masterPasswordSalt;

    public DatabaseService() {
        initializeDatabase();
    }
    
    public SecretKey deriveKeyFromPassword(String masterPassword) {
        try {
            // Get or create salt
            if (masterPasswordSalt == null) {
                String saltStr = getSetting("master_password_salt");
                if (saltStr == null || saltStr.isEmpty()) {
                    masterPasswordSalt = EncryptionService.generateSalt();
                    setSetting("master_password_salt", Base64.getEncoder().encodeToString(masterPasswordSalt));
                } else {
                    masterPasswordSalt = Base64.getDecoder().decode(saltStr);
                }
            }
            
            // Generate key from password and salt
            return EncryptionService.generateKeyFromPassword(masterPassword, masterPasswordSalt);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key: " + e.getMessage(), e);
        }
    }

    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String createPasswordsTable = """
            CREATE TABLE IF NOT EXISTS passwords (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                website TEXT,
                username TEXT NOT NULL,
                encrypted_password BLOB NOT NULL,
                encryption_iv BLOB NOT NULL,
                category TEXT DEFAULT 'General',
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """;
        
        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS app_settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPasswordsTable);
            stmt.execute(createSettingsTable);
            
            // Initialize master password and salt if not exists
            stmt.execute("INSERT OR IGNORE INTO app_settings (key, value) VALUES ('master_password_hash', ''), ('master_password_salt', '')");
        }
    }
    
    public String getSetting(String key) throws SQLException {
        String sql = "SELECT value FROM app_settings WHERE key = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        }
        return null;
    }

    public void setSetting(String key, String value) throws SQLException {
        String sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }

    public long addPassword(PasswordEntry password) throws SQLException {
        String sql = """
            INSERT INTO passwords (title, website, username, encrypted_password, 
            encryption_iv, category, notes, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, password.getTitle());
            pstmt.setString(2, password.getWebsite());
            pstmt.setString(3, password.getUsername());
            pstmt.setBytes(4, password.getEncryptedPassword());
            pstmt.setBytes(5, password.getEncryptionIv());
            pstmt.setString(6, password.getCategory());
            pstmt.setString(7, password.getNotes());
            pstmt.setLong(8, password.getCreatedAt());
            pstmt.setLong(9, password.getUpdatedAt());
            
            pstmt.executeUpdate();
            
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public void updatePassword(PasswordEntry password) throws SQLException {
        String sql = """
            UPDATE passwords 
            SET title = ?, website = ?, username = ?, encrypted_password = ?, 
                encryption_iv = ?, category = ?, notes = ?, updated_at = ?
            WHERE id = ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, password.getTitle());
            pstmt.setString(2, password.getWebsite());
            pstmt.setString(3, password.getUsername());
            pstmt.setBytes(4, password.getEncryptedPassword());
            pstmt.setBytes(5, password.getEncryptionIv());
            pstmt.setString(6, password.getCategory());
            pstmt.setString(7, password.getNotes());
            pstmt.setLong(8, System.currentTimeMillis());
            pstmt.setLong(9, password.getId());
            
            pstmt.executeUpdate();
        }
    }

    public void deletePassword(long id) throws SQLException {
        String sql = "DELETE FROM passwords WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<PasswordEntry> getAllPasswords() throws SQLException {
        List<PasswordEntry> passwords = new ArrayList<>();
        String sql = "SELECT * FROM passwords ORDER BY title ASC";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                PasswordEntry password = new PasswordEntry(
                    rs.getLong("id"),
                    rs.getString("title"),
                    rs.getString("website"),
                    rs.getString("username"),
                    rs.getBytes("encrypted_password"),
                    rs.getBytes("encryption_iv"),
                    rs.getString("category"),
                    rs.getString("notes"),
                    rs.getLong("created_at"),
                    rs.getLong("updated_at")
                );
                passwords.add(password);
            }
        }
        return passwords;
    }

    public List<PasswordEntry> searchPasswords(String query) throws SQLException {
        List<PasswordEntry> passwords = new ArrayList<>();
        String sql = """
            SELECT * FROM passwords 
            WHERE title LIKE ? OR website LIKE ? OR username LIKE ? 
            ORDER BY title ASC
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PasswordEntry password = new PasswordEntry(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("website"),
                        rs.getString("username"),
                        rs.getBytes("encrypted_password"),
                        rs.getBytes("encryption_iv"),
                        rs.getString("category"),
                        rs.getString("notes"),
                        rs.getLong("created_at"),
                        rs.getLong("updated_at")
                    );
                    passwords.add(password);
                }
            }
        }
        return passwords;
    }

    public void saveSetting(String key, String value) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO app_settings (key, value)
            VALUES (?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Change the master password: verify the old password, generate a new salt,
     * re-encrypt all stored passwords with a key derived from the new master password,
     * and update stored settings (salt and master_password_hash).
     * Returns true on success.
     */
    public synchronized boolean changeMasterPassword(String oldPassword, String newPassword) {
        try {
            // create a backup of the DB before making changes
            try {
                String dbPath = DB_URL.substring("jdbc:sqlite:".length());
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String backupName = dbPath + ".bak." + timestamp;
                Files.copy(Paths.get(dbPath), Paths.get(backupName), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Database backup created: " + backupName);
            } catch (Exception be) {
                // Log backup failure but continue (caller should have full backup ideally)
                System.err.println("Warning: failed to create DB backup before changing master password: " + be.getMessage());
            }

            // verify old password
            if (!verifyMasterPassword(oldPassword)) {
                return false;
            }

            // derive old key (uses current salt)
            javax.crypto.SecretKey oldKey = deriveKeyFromPassword(oldPassword);

            // generate new salt and persist it
            byte[] newSalt = EncryptionService.generateSalt();
            String newSaltB64 = Base64.getEncoder().encodeToString(newSalt);
            saveSetting("master_password_salt", newSaltB64);
            // update in-memory salt
            this.masterPasswordSalt = newSalt;

            // derive new key
            javax.crypto.SecretKey newKey = EncryptionService.generateKeyFromPassword(newPassword, newSalt);

            // Re-encrypt all password entries
            List<PasswordEntry> entries = getAllPasswords();
            for (PasswordEntry entry : entries) {
                try {
                    // decrypt with old key
                    EncryptionService.EncryptedData oldData = new EncryptionService.EncryptedData(
                        entry.getEncryptedPassword(), entry.getEncryptionIv());
                    String plain = EncryptionService.decryptData(oldData, oldKey);

                    // encrypt with new key
                    EncryptionService.EncryptedData newData = EncryptionService.encryptData(plain, newKey);

                    entry.setEncryptedPassword(newData.getEncryptedData());
                    entry.setEncryptionIv(newData.getIv());
                    updatePassword(entry);
                } catch (Exception e) {
                    System.err.println("Failed to re-encrypt entry id=" + entry.getId() + ": " + e.getMessage());
                    // continue with others
                }
            }

            // compute and store new master password hash (hashPassword reads stored salt)
            String newHash = hashPassword(newPassword);
            saveSetting("master_password_hash", newHash);

            return true;
        } catch (Exception e) {
            System.err.println("Error changing master password: " + e.getMessage());
            return false;
        }
    }
    
    public boolean verifyMasterPassword(String password) {
        try {
            String storedHash = getSetting("master_password_hash");
            if (storedHash == null || storedHash.isEmpty()) {
                // First time setup
                String hashedPassword = hashPassword(password);
                saveSetting("master_password_hash", hashedPassword);
                return true;
            }
            // Try current scheme (PBKDF2 with salt)
            try {
                String computed = hashPassword(password);
                if (storedHash.equals(computed)) {
                    return true;
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                // fall through to legacy checks
            }

            // Legacy: stored as plaintext password (older versions). Accept and migrate.
            if (storedHash.equals(password)) {
                try {
                    String newHash = hashPassword(password);
                    saveSetting("master_password_hash", newHash);
                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    // If migration fails, log and still allow login for compatibility
                    System.err.println("Warning: failed to migrate master password hash: " + e.getMessage());
                }
                return true;
            }

            // Legacy: maybe previously stored SHA-256 (base64) using EncryptionService.hashPassword
            try {
                String legacySha = EncryptionService.hashPassword(password);
                if (storedHash.equals(legacySha)) {
                    // migrate to PBKDF2-backed hash
                    try {
                        String newHash = hashPassword(password);
                        saveSetting("master_password_hash", newHash);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        System.err.println("Warning: failed to migrate legacy SHA hash: " + e.getMessage());
                    }
                    return true;
                }
            } catch (NoSuchAlgorithmException ignored) {
                // ignore
            }

            return false;
        } catch (SQLException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            System.err.println("Error verifying master password: " + e.getMessage());
            return false;
        }
    }
    
    private String hashPassword(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String storedSalt;
        try {
            storedSalt = getSetting("master_password_salt");
        } catch (SQLException e) {
            storedSalt = null;
        }

        // Generate new salt if not exists
        if (storedSalt == null || storedSalt.isEmpty()) {
            byte[] salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            storedSalt = Base64.getEncoder().encodeToString(salt);
            try {
                saveSetting("master_password_salt", storedSalt);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save salt", e);
            }
        }

        byte[] salt = Base64.getDecoder().decode(storedSalt);
        byte[] hash = EncryptionService.pbkdf2(password.toCharArray(), salt, 65536, 256);
        return Base64.getEncoder().encodeToString(hash);
    }
}