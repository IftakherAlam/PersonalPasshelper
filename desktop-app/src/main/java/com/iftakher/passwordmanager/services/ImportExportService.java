package com.iftakher.passwordmanager.services;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iftakher.passwordmanager.models.EncryptedPasswordDto;
import com.iftakher.passwordmanager.models.ExportData;
import com.iftakher.passwordmanager.models.PasswordEntry;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ImportExportService {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final int CURRENT_VERSION = 1;

    public static void exportToFile(List<PasswordEntry> passwords, String filePath, 
                                  SecretKey encryptionKey, String deviceName) throws Exception {
        // Convert passwords to DTOs
        List<EncryptedPasswordDto> passwordDtos = passwords.stream()
            .map(password -> {
                try {
                    // The password is already encrypted, we just need to encode for JSON
                    String encryptedData = Base64.getEncoder().encodeToString(password.getEncryptedPassword());
                    String iv = Base64.getEncoder().encodeToString(password.getEncryptionIv());
                    
                    return new EncryptedPasswordDto(
                        String.valueOf(password.getId()),
                        password.getTitle(),
                        password.getWebsite(),
                        password.getUsername(),
                        encryptedData,
                        iv,
                        password.getCategory(),
                        password.getNotes(),
                        password.getUpdatedAt()
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Error converting password to DTO", e);
                }
            })
            .collect(Collectors.toList());

        // Create export data
        ExportData exportData = new ExportData(
            CURRENT_VERSION,
            System.currentTimeMillis(),
            deviceName,
            passwordDtos
        );

        // Convert to JSON
        String json = gson.toJson(exportData);

        // Encrypt the JSON data
        EncryptionService.EncryptedData encrypted = EncryptionService.encryptData(json, encryptionKey);

        // Combine IV and encrypted data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(encrypted.getIv());
        outputStream.write(encrypted.getEncryptedData());

        // Write to file
        Files.write(Paths.get(filePath), outputStream.toByteArray());
    }

    public static List<PasswordEntry> importFromFile(String filePath, SecretKey encryptionKey) throws Exception {
        // Read file
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));

        // Separate IV and encrypted data
        byte[] iv = new byte[16];
        byte[] encryptedData = new byte[fileData.length - 16];
        
        System.arraycopy(fileData, 0, iv, 0, 16);
        System.arraycopy(fileData, 16, encryptedData, 0, encryptedData.length);

        // Decrypt data
        EncryptionService.EncryptedData encrypted = new EncryptionService.EncryptedData(encryptedData, iv);
        String json = EncryptionService.decryptData(encrypted, encryptionKey);

        // Parse JSON
        ExportData exportData = gson.fromJson(json, ExportData.class);

        // Convert DTOs back to PasswordEntry objects
        return exportData.getPasswords().stream()
            .map(dto -> {
                byte[] encryptedPassword = Base64.getDecoder().decode(dto.getEncryptedData());
                byte[] encryptionIv = Base64.getDecoder().decode(dto.getIv());
                
                return new PasswordEntry(
                    Long.parseLong(dto.getId()),
                    dto.getTitle(),
                    dto.getWebsite(),
                    dto.getUsername(),
                    encryptedPassword,
                    encryptionIv,
                    dto.getCategory(),
                    dto.getNotes(),
                    System.currentTimeMillis(), // Use current time for new entries
                    dto.getLastModified()
                );
            })
            .collect(Collectors.toList());
    }

    public static boolean isExportFile(String filePath) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            return fileData.length > 16; // Must have IV + some data
        } catch (Exception e) {
            return false;
        }
    }
}