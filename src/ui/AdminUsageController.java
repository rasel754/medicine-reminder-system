package ui;

import dao.MedicineDAO;
import dao.UsageLogDAO;
import dao.UserDAO;
import model.Medicine;
import model.UsageLog;
import service.AuthService;
import model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class AdminUsageController {

    @FXML
    private TableView<UsageRow> usageTable;

    @FXML
    private TableColumn<UsageRow, String> colMedName;

    @FXML
    private TableColumn<UsageRow, String> colUser;

    @FXML
    private TableColumn<UsageRow, String> colLogDate;

    @FXML
    private TableColumn<UsageRow, String> colStatus;

    private final UsageLogDAO usageLogDAO = new UsageLogDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // Security check
        User user = AuthService.getCurrentUser();
        if (user == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            System.err.println("Access Denied: Unprivileged access to AdminUsageController.");
            return;
        }

        colMedName.setCellValueFactory(new PropertyValueFactory<>("medName"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colLogDate.setCellValueFactory(new PropertyValueFactory<>("logDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add custom cell styling for compliance status
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("TAKEN")) {
                        setTextFill(Color.web("#10b981")); // Green
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("SKIPPED")) {
                        setTextFill(Color.web("#f59e0b")); // Yellow/Orange
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.web("#94a3b8")); // Secondary slate
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });

        loadUsageData();
    }

    private void loadUsageData() {
        List<UsageLog> logs = usageLogDAO.getAll();
        List<UsageRow> rows = new ArrayList<>();

        for (UsageLog log : logs) {
            Medicine med = medicineDAO.getById(log.getMedicineId());
            String medName = med != null ? med.getName() : "Unknown Medicine (" + log.getMedicineId() + ")";
            String username = "Unknown User";
            if (med != null) {
                username = userDAO.getUsernameById(med.getUserId());
            }

            rows.add(new UsageRow(
                medName,
                username,
                log.getTakenDate(),
                log.getStatus()
            ));
        }

        ObservableList<UsageRow> observableList = FXCollections.observableArrayList(rows);
        usageTable.setItems(observableList);
    }

    public static class UsageRow {
        private final String medName;
        private final String username;
        private final String logDate;
        private final String status;

        public UsageRow(String medName, String username, String logDate, String status) {
            this.medName = medName;
            this.username = username;
            this.logDate = logDate;
            this.status = status;
        }

        public String getMedName() { return medName; }
        public String getUsername() { return username; }
        public String getLogDate() { return logDate; }
        public String getStatus() { return status; }
    }
}
