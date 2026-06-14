package ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.AuthService;

import java.io.IOException;

/**
 * LoginController binds to resources/login.fxml, processes user login attempts,
 * and handles routing to the Dashboard views.
 */
public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setText("");
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        String role = authService.authenticateAndGetRole(username, password);
        if (role != null) {
            System.out.println("Login success: User is " + role);
            transitionToDashboard(role);
        } else {
            errorLabel.setText("Invalid username or password.");
        }
    }

    private void transitionToDashboard(String role) {
        try {
            String fxmlFile = role.equalsIgnoreCase("ADMIN") ? "/admin.fxml" : "/user.fxml";
            String titleRole = role.equalsIgnoreCase("ADMIN") ? "Admin Dashboard" : "User Dashboard";

            // Load role-specific dashboard FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();

            // Set new scene on active stage
            Stage stage = (Stage) usernameField.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("PillSync - " + titleRole + " (" + AuthService.getCurrentUser().getUsername() + ")");
            
            if (wasMaximized) {
                stage.setMaximized(true);
            } else if (wasFullScreen) {
                stage.setFullScreen(true);
            } else {
                stage.centerOnScreen();
            }
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load dashboard scene for role " + role + ": " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("System error loading dashboard view.");
        }
    }

    @FXML
    public void handleShowRegister(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) errorLabel.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("PillSync - Register");

            if (wasMaximized) {
                stage.setMaximized(true);
            } else if (wasFullScreen) {
                stage.setFullScreen(true);
            }
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load register scene: " + e.getMessage());
            e.printStackTrace();
            errorLabel.setText("System error loading register view.");
        }
    }
}
