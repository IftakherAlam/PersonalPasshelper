package com.iftakher.passwordmanager.controllers;

import com.iftakher.passwordmanager.controllers.MainController;
import com.iftakher.passwordmanager.services.DatabaseService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private PasswordField masterPasswordField;
    
    @FXML
    private Button loginButton;
    
    private Stage stage;
    private boolean authenticated = false;
    private DatabaseService databaseService;
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    public void setDatabaseService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLogin());
    }
    
    @FXML
    private void handleLogin() {
        String masterPassword = masterPasswordField.getText();
        if (masterPassword != null && !masterPassword.trim().isEmpty() && 
            databaseService.verifyMasterPassword(masterPassword)) {
            try {
                // Load main view
                authenticated = true;
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
                Parent mainRoot = loader.load();
                
                // Get controller and set dependencies
                MainController mainController = loader.getController();
                mainController.setDatabaseService(databaseService);
                
                // Set the encryption key derived from master password
                mainController.setEncryptionKey(databaseService.deriveKeyFromPassword(masterPassword));
                
                // Setup new stage for main window
                Stage mainStage = new Stage();
                mainStage.setTitle("Password Manager - Vault");
                mainStage.setScene(new Scene(mainRoot, 800, 600));
                mainStage.setMinWidth(600);
                mainStage.setMinHeight(400);
                
                // Add icon if available
                var iconStream = getClass().getResourceAsStream("/images/icon.png");
                if (iconStream != null) {
                    mainStage.getIcons().add(new Image(iconStream));
                }
                
                // Show main window and close login window
                mainStage.show();
                stage.close();
            } catch (Exception e) {
                // Show error dialog
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to open password manager: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            // Show error for invalid password
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Invalid master password");
            alert.showAndWait();
        }
    }
}