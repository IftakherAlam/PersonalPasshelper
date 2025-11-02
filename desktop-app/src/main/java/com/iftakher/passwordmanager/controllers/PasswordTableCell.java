package com.iftakher.passwordmanager.controllers;

import com.iftakher.passwordmanager.models.PasswordEntry;
import com.iftakher.passwordmanager.services.EncryptionService;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javax.crypto.SecretKey;

public class PasswordTableCell extends TableCell<PasswordEntry, byte[]> {
    private final TextField passwordField;
    private final Button toggleButton;
    private final HBox container;
    private final SecretKey encryptionKey;
    private boolean isVisible = false;

    public PasswordTableCell(SecretKey encryptionKey) {
        this.encryptionKey = encryptionKey;
        
        passwordField = new TextField();
        passwordField.setEditable(false);
        passwordField.getStyleClass().add("password-field");
        
        toggleButton = new Button("👁");
        toggleButton.setOnAction(e -> togglePasswordVisibility());
        
        container = new HBox(5); // 5 pixels spacing
        container.getChildren().addAll(passwordField, toggleButton);
        HBox.setHgrow(passwordField, Priority.ALWAYS);
    }

    private void togglePasswordVisibility() {
        if (getTableRow().getItem() == null) return;
        
        isVisible = !isVisible;
        updateItem(getTableRow().getItem().getEncryptedPassword(), false);
    }

    @Override
    protected void updateItem(byte[] encryptedPassword, boolean empty) {
        super.updateItem(encryptedPassword, empty);
        
        if (empty || encryptedPassword == null) {
            setGraphic(null);
            return;
        }

        PasswordEntry entry = getTableRow().getItem();
        if (entry == null) {
            setGraphic(null);
            return;
        }

        try {
            if (isVisible) {
                // Show decrypted password
                String decrypted = EncryptionService.decryptData(
                    new EncryptionService.EncryptedData(
                        entry.getEncryptedPassword(),
                        entry.getEncryptionIv()
                    ),
                    encryptionKey
                );
                passwordField.setText(decrypted);
            } else {
                // Show placeholder
                passwordField.setText("••••••••");
            }
            setGraphic(container);
        } catch (Exception e) {
            passwordField.setText("Error decrypting");
            setGraphic(container);
        }
    }
}