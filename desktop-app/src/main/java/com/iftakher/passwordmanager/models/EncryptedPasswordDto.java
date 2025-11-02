package com.iftakher.passwordmanager.models;


public class EncryptedPasswordDto {
    private String id;
    private String title;
    private String website;
    private String username;
    private String encryptedData;
    private String iv;
    private String category;
    private String notes;
    private long lastModified;

    public EncryptedPasswordDto() {}

    public EncryptedPasswordDto(String id, String title, String website, String username, 
                               String encryptedData, String iv, String category, 
                               String notes, long lastModified) {
        this.id = id;
        this.title = title;
        this.website = website;
        this.username = username;
        this.encryptedData = encryptedData;
        this.iv = iv;
        this.category = category;
        this.notes = notes;
        this.lastModified = lastModified;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEncryptedData() { return encryptedData; }
    public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }

    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}