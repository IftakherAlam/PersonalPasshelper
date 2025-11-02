package com.iftakher.passwordmanager.controllers;


import com.iftakher.passwordmanager.models.PasswordEntry;
import com.iftakher.passwordmanager.services.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.event.ActionEvent;

import javax.crypto.SecretKey;
import java.io.File;
import java.util.List;
import java.util.Optional;

public class MainController {
    @FXML private TableView<PasswordEntry> passwordsTable;
    @FXML private TableColumn<PasswordEntry, String> titleColumn;
    @FXML private TableColumn<PasswordEntry, String> usernameColumn;
    @FXML private TableColumn<PasswordEntry, byte[]> passwordColumn;
    @FXML private TableColumn<PasswordEntry, String> websiteColumn;
    @FXML private TableColumn<PasswordEntry, String> categoryColumn;
    
    @FXML private TextField searchField;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button exportButton;
    @FXML private Button importButton;
    @FXML private MenuItem logoutMenuItem;

    private ObservableList<PasswordEntry> passwords = FXCollections.observableArrayList();
    private DatabaseService databaseService;
    private SecretKey currentEncryptionKey;
    private String masterPasswordHash;

    public void initialize() {
        setupTableColumns();
        setupEventHandlers();
        loadPasswords();
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        websiteColumn.setCellValueFactory(new PropertyValueFactory<>("website"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        
        // Setup password column with custom cell factory
        passwordColumn.setCellValueFactory(new PropertyValueFactory<>("encryptedPassword"));
        passwordColumn.setCellFactory(col -> new PasswordTableCell(currentEncryptionKey));
        
        passwordsTable.setItems(passwords);
    }

    private void setupEventHandlers() {
        addButton.setOnAction(e -> showAddEditDialog(null));
        editButton.setOnAction(e -> {
            PasswordEntry selected = passwordsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAddEditDialog(selected);
            }
        });
        deleteButton.setOnAction(e -> deleteSelectedPassword());
        exportButton.setOnAction(e -> exportPasswords());
        importButton.setOnAction(e -> importPasswords());
        logoutMenuItem.setOnAction(e -> logout());
        
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterPasswords(newValue);
        });
    }

    private void loadPasswords() {
        try {
            if (databaseService == null) {
                databaseService = new DatabaseService();
            }
            List<PasswordEntry> passwordList = databaseService.getAllPasswords();
            passwords.setAll(passwordList);
        } catch (Exception e) {
            showError("Error loading passwords", e.getMessage());
        }
    }

    private void filterPasswords(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadPasswords();
            return;
        }

        try {
            List<PasswordEntry> filtered = databaseService.searchPasswords(query.trim());
            passwords.setAll(filtered);
        } catch (Exception e) {
            showError("Error searching passwords", e.getMessage());
        }
    }

    private void showAddEditDialog(PasswordEntry password) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/password-dialog.fxml"));
            Parent root = loader.load();
            
            PasswordDialogController controller = loader.getController();
            controller.setPasswordEntry(password);
            controller.setEncryptionKey(currentEncryptionKey);
            controller.setDatabaseService(databaseService);
            controller.setOnSaveCallback(this::loadPasswords);
            
            Stage stage = new Stage();
            controller.setDialogStage(stage);
            stage.setTitle(password == null ? "Add Password" : "Edit Password");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            
        } catch (Exception e) {
            showError("Error opening dialog", e.getMessage());
        }
    }

    private void deleteSelectedPassword() {
        PasswordEntry selected = passwordsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a password to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Password");
        alert.setContentText("Are you sure you want to delete '" + selected.getTitle() + "'?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                databaseService.deletePassword(selected.getId());
                loadPasswords();
            } catch (Exception e) {
                showError("Error deleting password", e.getMessage());
            }
        }
    }

    private void exportPasswords() {
        if (passwords.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Data", "There are no passwords to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Passwords");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Encrypted Files", "*.enc")
        );
        fileChooser.setInitialFileName("passwords_backup.enc");
        
        File file = fileChooser.showSaveDialog(passwordsTable.getScene().getWindow());
        if (file != null) {
            try {
                String deviceName = System.getProperty("os.name") + " - Desktop";
                ImportExportService.exportToFile(passwords, file.getAbsolutePath(), 
                                               currentEncryptionKey, deviceName);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", 
                         "Passwords exported successfully to: " + file.getAbsolutePath());
            } catch (Exception e) {
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void importPasswords() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Passwords");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Encrypted Files", "*.enc")
        );
        
        File file = fileChooser.showOpenDialog(passwordsTable.getScene().getWindow());
        if (file != null) {
            if (!ImportExportService.isExportFile(file.getAbsolutePath())) {
                showAlert(Alert.AlertType.ERROR, "Invalid File", 
                         "The selected file is not a valid password export file.");
                return;
            }

            try {
                List<PasswordEntry> importedPasswords = ImportExportService.importFromFile(
                    file.getAbsolutePath(), currentEncryptionKey);
                
                // Add imported passwords to database
                for (PasswordEntry password : importedPasswords) {
                    databaseService.addPassword(password);
                }
                
                loadPasswords();
                showAlert(Alert.AlertType.INFORMATION, "Import Successful", 
                         "Successfully imported " + importedPasswords.size() + " passwords.");
            } catch (Exception e) {
                showError("Import Failed", 
                         "Failed to import passwords. Please check your master password and try again.");
            }
        }
    }

    private void logout() {
        currentEncryptionKey = null;
        masterPasswordHash = null;
        passwords.clear();
        
        // Return to login screen
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) passwordsTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Password Manager - Login");
        } catch (Exception e) {
            showError("Error", "Failed to logout: " + e.getMessage());
        }
    }

    public void setEncryptionKey(SecretKey key) {
        this.currentEncryptionKey = key;
    }

    public void setMasterPasswordHash(String hash) {
        this.masterPasswordHash = hash;
    }

    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    // FXML action handlers referenced by main.fxml
    @FXML
    private void handleExit() {
        // Close the application
        Platform.exit();
    }

    @FXML
    private void showPasswordGenerator() {
        // Placeholder: show info that feature is not yet implemented
        showAlert(Alert.AlertType.INFORMATION, "Password Generator", "Password generator is not implemented yet.");
    }

    @FXML
    private void handleChangeMasterPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/change-master-password.fxml"));
            Parent root = loader.load();

            ChangeMasterPasswordController controller = loader.getController();
            controller.setDatabaseService(databaseService);

            Stage stage = new Stage();
            controller.setDialogStage(stage);
            stage.setTitle("Change Master Password");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (controller.isOkClicked()) {
                // update current encryption key to new password
                String newPass = controller.getNewPassword();
                this.currentEncryptionKey = databaseService.deriveKeyFromPassword(newPass);
                loadPasswords();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Master password changed successfully.");
            }
        } catch (Exception e) {
            showError("Error", "Failed to change master password: " + e.getMessage());
        }
    }
}