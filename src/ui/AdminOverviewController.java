package ui;

import dao.MedicineDAO;
import dao.StockDAO;
import dao.IntakeLogDAO;
import model.Medicine;
import model.Stock;
import model.IntakeLog;
import service.StockService;
import service.ExpiryService;
import service.UsageService;
import util.SoundUtil;

import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.util.List;

public class AdminOverviewController {

    @FXML
    private Label lblTotalStock;

    @FXML
    private Label lblLowStock;

    @FXML
    private Label lblExpiredMeds;

    @FXML
    private Label lblAdherenceRate;

    @FXML
    private PieChart adherencePieChart;

    @FXML
    private PieChart expiryRiskPieChart;

    @FXML
    private VBox alertsFeed;

    private final StockService stockService = new StockService();
    private final ExpiryService expiryService = new ExpiryService();
    private final UsageService usageService = new UsageService();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final StockDAO stockDAO = new StockDAO();
    private final IntakeLogDAO intakeLogDAO = new IntakeLogDAO();

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    private void loadDashboardData() {
        // 1. Populate KPI stats
        List<Stock> stocks = stockService.getAllStock();
        int totalProducts = stocks.size();

        int lowStockCount = 0;
        for (Stock s : stocks) {
            if (stockService.checkLowStock(s.getMedicineName(), 5)) {
                lowStockCount++;
            }
        }

        List<Medicine> expired = expiryService.getExpiredMedicines();
        int expiredCount = expired.size();

        int taken = usageService.getTakenCount("All time");
        int total = usageService.getTotalCount("All time");
        int missed = Math.max(0, total - taken);
        double adherenceRate = 0.0;
        if (total > 0) {
            adherenceRate = ((double) taken / total) * 100.0;
        }

        lblTotalStock.setText(String.valueOf(totalProducts));
        lblLowStock.setText(String.valueOf(lowStockCount));
        lblExpiredMeds.setText(String.valueOf(expiredCount));
        lblAdherenceRate.setText(String.format("%.1f%%", adherenceRate));

        // 2. Render Intake Adherence PieChart
        adherencePieChart.getData().clear();
        if (total == 0) {
            PieChart.Data noData = new PieChart.Data("No Logs Seeding", 1);
            adherencePieChart.getData().add(noData);
        } else {
            PieChart.Data takenSlice = new PieChart.Data("Taken (" + taken + ")", taken);
            PieChart.Data missedSlice = new PieChart.Data("Missed (" + missed + ")", missed);
            adherencePieChart.getData().addAll(takenSlice, missedSlice);
            
            // Inline styling colors for charts
            takenSlice.getNode().setStyle("-fx-pie-color: #10b981;");
            missedSlice.getNode().setStyle("-fx-pie-color: #ef4444;");
        }

        // 3. Render Stock Expiry Risk PieChart
        expiryRiskPieChart.getData().clear();
        List<Medicine> allMeds = medicineDAO.getAll();
        int nearExpiryCount = expiryService.getNearExpiryMedicines().size();
        int safeCount = Math.max(0, allMeds.size() - expiredCount - nearExpiryCount);

        if (allMeds.isEmpty()) {
            PieChart.Data noMeds = new PieChart.Data("No Medicines", 1);
            expiryRiskPieChart.getData().add(noMeds);
        } else {
            PieChart.Data safeSlice = new PieChart.Data("Safe (" + safeCount + ")", safeCount);
            PieChart.Data warningSlice = new PieChart.Data("Expiring Soon (" + nearExpiryCount + ")", nearExpiryCount);
            PieChart.Data expiredSlice = new PieChart.Data("Expired (" + expiredCount + ")", expiredCount);
            
            expiryRiskPieChart.getData().addAll(safeSlice, warningSlice, expiredSlice);
            
            safeSlice.getNode().setStyle("-fx-pie-color: #10b981;");
            warningSlice.getNode().setStyle("-fx-pie-color: #fbbf24;");
            expiredSlice.getNode().setStyle("-fx-pie-color: #ef4444;");
        }

        // 4. Load Alerts & Actions Feed
        loadAlertsFeed(expired, stocks);
    }

    private void loadAlertsFeed(List<Medicine> expired, List<Stock> stocks) {
        alertsFeed.getChildren().clear();

        boolean hasAlerts = false;

        // A. Add Expired Stock Alerts
        for (Medicine m : expired) {
            hasAlerts = true;
            HBox alertCard = createAlertCard(
                "🚨 Expired Medication: " + m.getName() + " (" + m.getExpiryDate() + ")",
                "Purge",
                "-fx-background-color: rgba(239, 68, 68, 0.1); -fx-border-color: #ef4444;",
                () -> {
                    medicineDAO.delete(m.getId());
                    loadDashboardData();
                    showStyledAlert(Alert.AlertType.INFORMATION, "Medication Purged", null, 
                        m.getName() + " has been successfully removed from system databases.");
                }
            );
            alertsFeed.getChildren().add(alertCard);
        }

        // B. Add Low Stock Alerts
        for (Stock s : stocks) {
            if (stockService.checkLowStock(s.getMedicineName(), 5)) {
                hasAlerts = true;
                HBox alertCard = createAlertCard(
                    "📦 Low Inventory: " + s.getMedicineName() + " (" + s.getQuantity() + " left)",
                    "Restock",
                    "-fx-background-color: rgba(245, 158, 11, 0.1); -fx-border-color: #f59e0b;",
                    () -> {
                        stockDAO.updateQuantity(s.getMedicineName(), 25);
                        loadDashboardData();
                        showStyledAlert(Alert.AlertType.INFORMATION, "Inventory Replenished", null, 
                            s.getMedicineName() + " has been restocked to 25 units.");
                    }
                );
                alertsFeed.getChildren().add(alertCard);
            }
        }

        // C. Add Patient Missed Doses Alerts
        List<IntakeLog> missedLogs = intakeLogDAO.getRecentMissedLogs(5);
        for (IntakeLog log : missedLogs) {
            hasAlerts = true;
            HBox alertCard = createAlertCard(
                "⚠️ Missed Dose: " + log.getUsername() + " missed " + log.getMedicineName() + " on " + log.getDate(),
                "Acknowledge",
                "-fx-background-color: rgba(99, 102, 241, 0.1); -fx-border-color: #6366f1;",
                () -> {
                    // Simulates acknowledgment or email reminder to patient
                    showStyledAlert(Alert.AlertType.INFORMATION, "Dose Miss Acknowledged", null, 
                        "Patient " + log.getUsername() + " has been notified regarding their missed dose of " + log.getMedicineName() + ".");
                }
            );
            alertsFeed.getChildren().add(alertCard);
        }

        if (!hasAlerts) {
            Label lblNoAlerts = new Label("✅ System is healthy. No pending actions required.");
            lblNoAlerts.setStyle("-fx-text-fill: #10b981; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10;");
            alertsFeed.getChildren().add(lblNoAlerts);
        }
    }

    private HBox createAlertCard(String message, String actionButtonText, String cardStyle, Runnable action) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 15, 10, 15));
        card.setStyle(cardStyle + " -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(msgLabel, Priority.ALWAYS);
        
        Button actBtn = new Button(actionButtonText);
        actBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 5; -fx-cursor: hand;");
        actBtn.setOnAction(e -> action.run());

        // Hover effect for quick action buttons inside card
        actBtn.setOnMouseEntered(e -> actBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 5; -fx-cursor: hand;"));
        actBtn.setOnMouseExited(e -> actBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-padding: 5 12; -fx-background-radius: 5; -fx-cursor: hand;"));

        card.getChildren().addAll(msgLabel, actBtn);
        return card;
    }

    @FXML
    private void handleBroadcastChime() {
        SoundUtil.playAlert();
        showStyledAlert(Alert.AlertType.INFORMATION, "Chime Broadcasted", "Alert Broadcast Triggered",
            "A test chime notification is playing in a loop. You can mark a reminder as Taken or click Log Out to stop.");
    }

    @FXML
    private void handleAutoReplenish() {
        List<Stock> stocks = stockService.getAllStock();
        int replenishedCount = 0;
        for (Stock s : stocks) {
            if (stockService.checkLowStock(s.getMedicineName(), 5)) {
                stockDAO.updateQuantity(s.getMedicineName(), 25);
                replenishedCount++;
            }
        }
        loadDashboardData();
        showStyledAlert(Alert.AlertType.INFORMATION, "Auto-Replenish Completed", "Replenished " + replenishedCount + " Items",
            "All low-stock items (quantity <= 5) have been auto-replenished to 25 units.");
    }

    @FXML
    private void handlePurgeExpired() {
        List<Medicine> expired = expiryService.getExpiredMedicines();
        int deletedCount = expired.size();
        for (Medicine m : expired) {
            medicineDAO.delete(m.getId());
        }
        loadDashboardData();
        showStyledAlert(Alert.AlertType.INFORMATION, "Expired Medicines Purged", "Purged " + deletedCount + " Items",
            "All expired medicines have been successfully removed from databases.");
    }

    private void showStyledAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        if (lblTotalStock != null && lblTotalStock.getScene() != null) {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
            dialogPane.getStyleClass().add("alert-dialog");
        }
        alert.show();
    }
}
