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
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;

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
    private Button btnIntakeHistory;

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
        
        // Start background reminders for user
        if (MainApp.getReminderService() != null) {
            MainApp.getReminderService().startChecker();
        }
        
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

                // Initialize Today's Intake TableView
                TableView<Medicine> intakeTable = (TableView<Medicine>) pane.lookup("#intakeTable");
                if (intakeTable != null && intakeTable.getColumns().size() >= 4) {
                    TableColumn<Medicine, String> colName = (TableColumn<Medicine, String>) intakeTable.getColumns().get(0);
                    TableColumn<Medicine, String> colDosage = (TableColumn<Medicine, String>) intakeTable.getColumns().get(1);
                    TableColumn<Medicine, String> colTime = (TableColumn<Medicine, String>) intakeTable.getColumns().get(2);
                    TableColumn<Medicine, Void> colActions = (TableColumn<Medicine, Void>) intakeTable.getColumns().get(3);

                    if (colName != null) colName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("name"));
                    if (colDosage != null) colDosage.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dosage"));
                    if (colTime != null) colTime.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("time"));

                    if (colActions != null) {
                        colActions.setCellFactory(param -> new TableCell<Medicine, Void>() {
                            private final Button btnTaken = new Button("✔ Taken");
                            private final Button btnMissed = new Button("❌ Missed");
                            private final HBox container = new HBox(10, btnTaken, btnMissed);

                            {
                                btnTaken.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                                btnMissed.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                                container.setAlignment(Pos.CENTER);

                                btnTaken.setOnAction(evt -> {
                                    Medicine med = getTableView().getItems().get(getIndex());
                                    handleMarkIntake(med, "taken");
                                });

                                btnMissed.setOnAction(evt -> {
                                    Medicine med = getTableView().getItems().get(getIndex());
                                    handleMarkIntake(med, "missed");
                                });
                            }

                            @Override
                            protected void updateItem(Void item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty) {
                                    setGraphic(null);
                                } else {
                                    setGraphic(container);
                                }
                            }
                        });
                    }
                    loadIntakeTableData(intakeTable, user.getId());
                }
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

        // Stop background reminders when logging out
        if (MainApp.getReminderService() != null) {
            MainApp.getReminderService().stopChecker();
        }

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
        if (btnIntakeHistory != null) btnIntakeHistory.getStyleClass().remove("nav-button-active");

        if (activeBtn != null) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    private void loadIntakeTableData(TableView<Medicine> intakeTable, int userId) {
        List<Medicine> list = medicineService.getMedicinesForUser(userId);
        // Display user's scheduled medicines for today (filtering out expired ones is a good health-check practice)
        list.removeIf(stockService::isExpired);
        intakeTable.setItems(javafx.collections.FXCollections.observableArrayList(list));
    }

    private void handleMarkIntake(Medicine med, String status) {
        User user = AuthService.getCurrentUser();
        if (user == null) return;

        System.out.println("\n--- Intake Track Button Clicked ---");
        System.out.println("Medicine: " + med.getName() + " (ID: " + med.getId() + ")");
        System.out.println("Mark Action: " + status.toUpperCase());
        System.out.println("User: " + user.getUsername() + " (ID: " + user.getId() + ")");

        dao.IntakeLogDAO intakeLogDAO = new dao.IntakeLogDAO();
        String today = java.time.LocalDate.now().toString();

        // Check duplicate entry for same medicine on same date
        if (intakeLogDAO.checkExists(user.getId(), med.getId(), today)) {
            System.out.println("Validation Check: Already marked for today. Preventing duplicate.");
            Alert alert = new Alert(Alert.AlertType.WARNING, "You have already marked '" + med.getName() + "' for today!", ButtonType.OK);
            alert.setHeaderText("Already Marked");
            alert.showAndWait();
            return;
        }

        boolean success = intakeLogDAO.insertLog(user.getId(), med.getId(), status);
        if (success) {
            System.out.println("Intake log successfully recorded in database.");
            
            // Deduct stock levels by 1 if marked as Taken
            if (status.equalsIgnoreCase("taken")) {
                boolean stockReduced = stockService.reduceStock(med.getName(), 1);
                System.out.println("Inventory reduction status: " + (stockReduced ? "Stock reduced by 1" : "Stock reduction skipped/failed"));
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Marked as " + (status.equalsIgnoreCase("taken") ? "Taken" : "Missed") + " successfully!", ButtonType.OK);
            alert.setHeaderText("Record Logged");
            alert.showAndWait();
        } else {
            System.err.println("Error: Failed to insert intake log into database.");
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to record log. See logs for details.", ButtonType.OK);
            alert.setHeaderText("Database Error");
            alert.showAndWait();
        }
    }

    @FXML
    public void showIntakeHistoryPane(ActionEvent event) {
        highlightActiveButton(btnIntakeHistory);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/intake_history.fxml"));
            Parent pane = loader.load();

            TableView<model.IntakeLog> table = (TableView<model.IntakeLog>) pane.lookup("#intakeHistoryTable");
            if (table != null && table.getColumns().size() >= 4) {
                TableColumn<model.IntakeLog, String> colName = (TableColumn<model.IntakeLog, String>) table.getColumns().get(0);
                TableColumn<model.IntakeLog, String> colDosage = (TableColumn<model.IntakeLog, String>) table.getColumns().get(1);
                TableColumn<model.IntakeLog, String> colStatus = (TableColumn<model.IntakeLog, String>) table.getColumns().get(2);
                TableColumn<model.IntakeLog, String> colDate = (TableColumn<model.IntakeLog, String>) table.getColumns().get(3);

                if (colName != null) colName.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("medicineName"));
                if (colDosage != null) colDosage.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("medicineDosage"));
                if (colStatus != null) {
                    colStatus.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
                    colStatus.setCellFactory(column -> new TableCell<model.IntakeLog, String>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                setText(item.toUpperCase());
                                if (item.equalsIgnoreCase("taken")) {
                                    setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                                } else {
                                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                                }
                            }
                        }
                    });
                }
                if (colDate != null) colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));

                User user = AuthService.getCurrentUser();
                if (user != null) {
                    dao.IntakeLogDAO dao = new dao.IntakeLogDAO();
                    List<model.IntakeLog> logs = dao.getLogsByUserId(user.getId());
                    table.setItems(javafx.collections.FXCollections.observableArrayList(logs));
                }
            }

            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            System.err.println("Failed to load intake history FXML: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
