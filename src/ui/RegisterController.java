package ui;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.AuthService;

import java.io.IOException;

/**
 * RegisterController handles user registration operations, field validations,
 * and navigates between the login and registration interfaces.
 */
public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label statusLabel;

    @FXML
    private Button registerButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Clear potential previous errors
        statusLabel.setText("");
        
        // Add roles "user" and "admin"
        roleComboBox.getItems().addAll("user", "admin");
        roleComboBox.setValue("user"); // Set default selection
    }

    @FXML
    public void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            statusLabel.setTextFill(Paint.valueOf("#ef4444")); // Red color
            statusLabel.setText("Please enter both username and password.");
            return;
        }

        // Check if username already exists
        if (authService.usernameExists(username)) {
            statusLabel.setTextFill(Paint.valueOf("#ef4444"));
            statusLabel.setText("Username is already taken.");
            return;
        }

        // Register user with database (storing uppercase role consistency)
        boolean success = authService.register(username, password, role.toUpperCase());
        if (success) {
            statusLabel.setTextFill(Paint.valueOf("#10b981")); // Green color
            statusLabel.setText("Registration successful! Redirecting...");
            
            // Disable controls to prevent double submissions during transition
            registerButton.setDisable(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            roleComboBox.setDisable(true);

            // Wait 1.5 seconds and transition back to login screen
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> loadLoginScene());
            pause.play();
        } else {
            statusLabel.setTextFill(Paint.valueOf("#ef4444"));
            statusLabel.setText("Registration failed. Please try again.");
        }
    }

    @FXML
    public void handleShowLogin(ActionEvent event) {
        loadLoginScene();
    }

    private void loadLoginScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("PillSync - Login");
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load login scene: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setTextFill(Paint.valueOf("#ef4444"));
            statusLabel.setText("System error loading login view.");
        }
    }
}
