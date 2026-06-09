package ui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import service.AuthService;
import service.MedicineService;
import service.StockService;
import model.Medicine;
import model.User;

import java.io.IOException;
import java.util.List;

/**
 * UserController coordinates USER navigation tabs, updates statistics,
 * and swaps content panes dynamically in the user dashboard.
 */
public class UserController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnAddMedicine;

    @FXML
    private Button btnMedicineList;

    @FXML
    private Button btnCalendar;

    @FXML
    private StackPane contentArea;

    private final MedicineService medicineService = new MedicineService();
    private final StockService stockService = new StockService();

    @FXML
    public void initialize() {
        User user = AuthService.getCurrentUser();
        
        // Security check: Assert user is logged in and has the USER role
        if (user == null || !user.getRole().equalsIgnoreCase("USER")) {
            System.err.println("Access Denied: Logged in user is not a USER.");
            Platform.runLater(this::redirectToLoginWithAccessDenied);
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
        
        // Show dashboard overview pane on load
        showDashboardPane(null);
    }

    private void redirectToLoginWithAccessDenied() {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Access Denied. You do not have permission to view this page.", ButtonType.OK);
            alert.setHeaderText("Security Alert");
            alert.showAndWait();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = null;
            if (contentArea != null && contentArea.getScene() != null) {
                stage = (Stage) contentArea.getScene().getWindow();
            }
            
            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle("PillSync - Login");
                stage.centerOnScreen();
                stage.show();
            }
        } catch (IOException e) {
            System.err.println("Failed to redirect to login: " + e.getMessage());
        }
    }

    @FXML
    public void showDashboardPane(ActionEvent event) {
        highlightActiveButton(btnDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard_overview.fxml"));
            Parent pane = loader.load();

            // Find elements inside loaded pane using lookup
            Label lblTotalMedicines = (Label) pane.lookup("#lblTotalMedicines");
            Label lblExpiringSoon = (Label) pane.lookup("#lblExpiringSoon");
            Label lblLowStock = (Label) pane.lookup("#lblLowStock");

            // Update stats from services
            User user = AuthService.getCurrentUser();
            if (user != null) {
                List<Medicine> meds = medicineService.getMedicinesForUser(user.getId());
                int totalMeds = meds.size();
                int expiringCount = 0;
                int lowStockCount = 0;

                for (Medicine m : meds) {
                    if (stockService.isExpired(m)) {
                        expiringCount++;
                    }
                    if (stockService.checkLowStock(m.getName(), 5)) {
                        lowStockCount++;
                    }
                }

                if (lblTotalMedicines != null) lblTotalMedicines.setText(String.valueOf(totalMeds));
                if (lblExpiringSoon != null) lblExpiringSoon.setText(String.valueOf(expiringCount));
                if (lblLowStock != null) lblLowStock.setText(String.valueOf(lowStockCount));
            }

            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            System.err.println("Failed to load dashboard overview FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showAddMedicinePane(ActionEvent event) {
        highlightActiveButton(btnAddMedicine);
        loadPane("/medicine_form.fxml");
    }

    @FXML
    public void showMedicineListPane(ActionEvent event) {
        highlightActiveButton(btnMedicineList);
        loadPane("/medicine_list.fxml");
    }

    @FXML
    public void showCalendarPane(ActionEvent event) {
        highlightActiveButton(btnCalendar);
        loadPane("/calendar_view.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        AuthService authService = new AuthService();
        authService.logout();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("PillSync - Login");
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load login FXML: " + e.getMessage());
        }
    }

    private void loadPane(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent pane = loader.load();
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            System.err.println("Failed to load pane " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void highlightActiveButton(Button activeBtn) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("nav-button-active");
        if (btnAddMedicine != null) btnAddMedicine.getStyleClass().remove("nav-button-active");
        if (btnMedicineList != null) btnMedicineList.getStyleClass().remove("nav-button-active");
        if (btnCalendar != null) btnCalendar.getStyleClass().remove("nav-button-active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }
}
