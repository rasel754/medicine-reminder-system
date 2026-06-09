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
import service.StockService;
import dao.MedicineDAO;
import model.Medicine;
import model.Stock;
import model.User;

import java.io.IOException;
import java.util.List;

/**
 * AdminController coordinates ADMIN navigation tabs, updates pharmacy statistics,
 * and swaps content panes dynamically in the admin dashboard view.
 */
public class AdminController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnStock;

    @FXML
    private Button btnExpiry;

    @FXML
    private Button btnUsage;

    @FXML
    private StackPane contentArea;

    private final StockService stockService = new StockService();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    @FXML
    public void initialize() {
        User user = AuthService.getCurrentUser();
        
        // Security check: Assert user is logged in and has the ADMIN role
        if (user == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            System.err.println("Access Denied: Logged in user is not an ADMIN.");
            Platform.runLater(this::redirectToLoginWithAccessDenied);
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
        
        // Show admin dashboard overview pane on load
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/admin_overview.fxml"));
            Parent pane = loader.load();

            // Find elements inside loaded pane using lookup
            Label lblTotalStock = (Label) pane.lookup("#lblTotalStock");
            Label lblLowStock = (Label) pane.lookup("#lblLowStock");
            Label lblExpiredMeds = (Label) pane.lookup("#lblExpiredMeds");

            // Calculate metrics
            List<Stock> stocks = stockService.getAllStock();
            int totalProducts = stocks.size();
            
            int lowStockCount = 0;
            for (Stock s : stocks) {
                if (stockService.checkLowStock(s.getMedicineName(), 5)) {
                    lowStockCount++;
                }
            }

            List<Medicine> meds = medicineDAO.getAll();
            int expiredCount = 0;
            for (Medicine m : meds) {
                if (stockService.isExpired(m)) {
                    expiredCount++;
                }
            }

            if (lblTotalStock != null) lblTotalStock.setText(String.valueOf(totalProducts));
            if (lblLowStock != null) lblLowStock.setText(String.valueOf(lowStockCount));
            if (lblExpiredMeds != null) lblExpiredMeds.setText(String.valueOf(expiredCount));

            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            System.err.println("Failed to load admin overview FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showStockPane(ActionEvent event) {
        highlightActiveButton(btnStock);
        loadPane("/admin_stock.fxml");
    }

    @FXML
    public void showExpiryPane(ActionEvent event) {
        highlightActiveButton(btnExpiry);
        loadPane("/admin_expiry.fxml");
    }

    @FXML
    public void showUsagePane(ActionEvent event) {
        highlightActiveButton(btnUsage);
        loadPane("/admin_usage.fxml");
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
        if (btnStock != null) btnStock.getStyleClass().remove("nav-button-active");
        if (btnExpiry != null) btnExpiry.getStyleClass().remove("nav-button-active");
        if (btnUsage != null) btnUsage.getStyleClass().remove("nav-button-active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }
}
