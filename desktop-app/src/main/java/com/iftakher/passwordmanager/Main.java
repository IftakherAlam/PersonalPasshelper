package com.iftakher.passwordmanager;


import com.iftakher.passwordmanager.controllers.LoginController;
import com.iftakher.passwordmanager.services.DatabaseService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private DatabaseService databaseService;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize database
            databaseService = new DatabaseService();
            
            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();
            
            // Pass database service to controller
            LoginController controller = loader.getController();
            controller.setDatabaseService(databaseService);
            controller.setStage(primaryStage);
            
            // Setup stage
            primaryStage.setTitle("Password Manager");
            primaryStage.setScene(new Scene(root, 800, 600));
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(400);
            
            // Add icon if available
            var iconStream = getClass().getResourceAsStream("/images/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
            
            primaryStage.show();
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            showErrorDialog("Startup Error", "Failed to start Password Manager: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        // Clean up resources
        if (databaseService != null) {
            databaseService.close();
        }
        logger.info("Application stopped");
    }

    private void showErrorDialog(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // Set system properties for better performance
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        logger.info("Starting Password Manager Desktop Application");
        launch(args);
    }
}