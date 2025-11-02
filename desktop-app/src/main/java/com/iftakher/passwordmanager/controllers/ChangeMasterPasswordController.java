package com.iftakher.passwordmanager.controllers;

import com.iftakher.passwordmanager.services.DatabaseService;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class ChangeMasterPasswordController {
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private Stage dialogStage;
    private DatabaseService databaseService;
    private boolean okClicked = false;
    private String newPassword;

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public boolean isOkClicked() {
        return okClicked;
    }

    public String getNewPassword() {
        return newPassword;
    }

    @FXML
    private void handleOk() {
        String current = currentPasswordField.getText();
        String next = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current == null || current.isBlank()) {
            showAlert("Current password is required");
            return;
        }
        if (next == null || next.length() < 6) {
            showAlert("New password must be at least 6 characters");
            return;
        }
        if (!next.equals(confirm)) {
            showAlert("New password and confirm password do not match");
            return;
        }

        try {
            boolean ok = databaseService.changeMasterPassword(current, next);
            if (ok) {
                this.newPassword = next;
                this.okClicked = true;
                dialogStage.close();
            } else {
                showAlert("Current master password is incorrect or change failed");
            }
        } catch (Exception e) {
            showAlert("Error changing master password: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Change Master Password");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
