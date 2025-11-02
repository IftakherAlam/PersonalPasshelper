package com.iftakher.passwordmanager.models;

import java.util.Arrays;
import java.util.Objects;

public class PasswordEntry {
    private long id;
    private String title;
    private String website;
    private String username;
    private byte[] encryptedPassword;
    private byte[] encryptionIv;
    private String category;
    private String notes;
    private long createdAt;
    private long updatedAt;

    public PasswordEntry(long id, String title, String website, String username, 
                        byte[] encryptedPassword, byte[] encryptionIv, String category, 
                        String notes, long createdAt, long updatedAt) {
        this.id = id;
        this.title = title;
        this.website = website;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.encryptionIv = encryptionIv;
        this.category = category;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PasswordEntry(String title, String website, String username, 
                        byte[] encryptedPassword, byte[] encryptionIv, String category, String notes) {
        this(0, title, website, username, encryptedPassword, encryptionIv, category, notes,
             System.currentTimeMillis(), System.currentTimeMillis());
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public byte[] getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(byte[] encryptedPassword) { this.encryptedPassword = encryptedPassword; }

    public byte[] getEncryptionIv() { return encryptionIv; }
    public void setEncryptionIv(byte[] encryptionIv) { this.encryptionIv = encryptionIv; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordEntry that = (PasswordEntry) o;
        return id == that.id &&
                createdAt == that.createdAt &&
                updatedAt == that.updatedAt &&
                Objects.equals(title, that.title) &&
                Objects.equals(website, that.website) &&
                Objects.equals(username, that.username) &&
                Arrays.equals(encryptedPassword, that.encryptedPassword) &&
                Arrays.equals(encryptionIv, that.encryptionIv) &&
                Objects.equals(category, that.category) &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, title, website, username, category, notes, createdAt, updatedAt);
        result = 31 * result + Arrays.hashCode(encryptedPassword);
        result = 31 * result + Arrays.hashCode(encryptionIv);
        return result;
    }

    // For display purposes - returns decrypted password
    private String decryptedPassword;
    
    public String getPassword() { 
        return decryptedPassword; 
    }
    
    public void setPassword(String password) { 
        this.decryptedPassword = password;
    }
    
    @Override
    public String toString() {
        return "PasswordEntry{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", website='" + website + '\'' +
                ", username='" + username + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}