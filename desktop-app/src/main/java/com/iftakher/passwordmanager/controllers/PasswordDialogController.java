package com.iftakher.passwordmanager.controllers;

import com.iftakher.passwordmanager.models.PasswordEntry;
import com.iftakher.passwordmanager.services.DatabaseService;
import com.iftakher.passwordmanager.services.EncryptionService;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class PasswordDialogController {
    @FXML
    private TextField titleField;
    
    @FXML
    private TextField websiteField;
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private TextField passwordField;
    
    @FXML
    private ComboBox<String> categoryComboBox;
    
    @FXML
    private TextArea notesArea;
    
    private Stage dialogStage;
    private PasswordEntry passwordEntry;
    private boolean okClicked = false;
    private SecretKey encryptionKey;
    private DatabaseService databaseService;
    private Runnable onSaveCallback;
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }
    
    @FXML
    public void initialize() {
        // Initialize category combo box with default categories
        categoryComboBox.getItems().addAll(
            "General", "Email", "Banking", "Social Media", "Shopping", "Work", "Other"
        );
        categoryComboBox.setValue("General");
    }

    private void clearFields() {
        titleField.setText("");
        websiteField.setText("");
        usernameField.setText("");
        passwordField.setText("");
        categoryComboBox.setValue("General");
        notesArea.setText("");
    }
    
    public void setPasswordEntry(PasswordEntry passwordEntry) {
        if (passwordEntry == null) {
            // Initialize a new password entry for adding
            clearFields();
            this.passwordEntry = new PasswordEntry("", "", "", null, null, "General", "");
            return;
        }
        
        this.passwordEntry = passwordEntry;
        titleField.setText(passwordEntry.getTitle());
        websiteField.setText(passwordEntry.getWebsite());
        usernameField.setText(passwordEntry.getUsername());
        
        if (encryptionKey != null && passwordEntry.getEncryptedPassword() != null) {
            try {
                IvParameterSpec ivSpec = new IvParameterSpec(passwordEntry.getEncryptionIv());
                Cipher cipher = Cipher.getInstance(EncryptionService.ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, encryptionKey, ivSpec);
                byte[] decrypted = cipher.doFinal(passwordEntry.getEncryptedPassword());
                String decryptedPassword = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
                passwordField.setText(decryptedPassword);
            } catch (Exception e) {
                System.err.println("Error decrypting password: " + e.getMessage());
                passwordField.setText("");
            }
        }
        
        categoryComboBox.setValue(passwordEntry.getCategory());
        notesArea.setText(passwordEntry.getNotes());
    }
    
    public boolean isOkClicked() {
        return okClicked;
    }
    
        public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    public void setEncryptionKey(SecretKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
    
    @FXML
    private void handleOk() {
        if (isInputValid()) {
            try {
                passwordEntry.setTitle(titleField.getText());
                passwordEntry.setWebsite(websiteField.getText());
                passwordEntry.setUsername(usernameField.getText());
                
                // Encrypt the password before saving
                EncryptionService.EncryptedData encryptedData = 
                    EncryptionService.encryptData(passwordField.getText(), encryptionKey);
                
                passwordEntry.setEncryptedPassword(encryptedData.getEncryptedData());
                passwordEntry.setEncryptionIv(encryptedData.getIv());
                passwordEntry.setCategory(categoryComboBox.getValue());
                passwordEntry.setNotes(notesArea.getText());
                
                if (passwordEntry.getId() == 0) {
                    databaseService.addPassword(passwordEntry);
                } else {
                    databaseService.updatePassword(passwordEntry);
                }
                
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
                
                okClicked = true;
                dialogStage.close();
            } catch (Exception e) {
                System.err.println("Error saving password: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
    
    private boolean isInputValid() {
        // TODO: Add validation logic
        return titleField.getText() != null && !titleField.getText().isEmpty() &&
               usernameField.getText() != null && !usernameField.getText().isEmpty() &&
               passwordField.getText() != null && !passwordField.getText().isEmpty();
    }
}