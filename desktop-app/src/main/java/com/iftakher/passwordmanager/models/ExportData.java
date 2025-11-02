package com.iftakher.passwordmanager.models;

import java.util.List;

public class ExportData {
    private int version;
    private long exportDate;
    private String deviceName;
    private List<EncryptedPasswordDto> passwords;

    public ExportData() {}

    public ExportData(int version, long exportDate, String deviceName, List<EncryptedPasswordDto> passwords) {
        this.version = version;
        this.exportDate = exportDate;
        this.deviceName = deviceName;
        this.passwords = passwords;
    }

    // Getters and Setters
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public long getExportDate() { return exportDate; }
    public void setExportDate(long exportDate) { this.exportDate = exportDate; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public List<EncryptedPasswordDto> getPasswords() { return passwords; }
    public void setPasswords(List<EncryptedPasswordDto> passwords) { this.passwords = passwords; }
}