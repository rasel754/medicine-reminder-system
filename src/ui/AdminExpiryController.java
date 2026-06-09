package ui;

import dao.MedicineDAO;
import dao.UserDAO;
import model.Medicine;
import service.StockService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class AdminExpiryController {

    @FXML
    private TableView<ExpiryRow> expiryTable;

    @FXML
    private TableColumn<ExpiryRow, String> colName;

    @FXML
    private TableColumn<ExpiryRow, String> colDosage;

    @FXML
    private TableColumn<ExpiryRow, String> colExpiry;

    @FXML
    private TableColumn<ExpiryRow, String> colUser;

    @FXML
    private TableColumn<ExpiryRow, String> colStatus;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final UserDAO userDAO = new UserDAO();
    private final StockService stockService = new StockService();

    @FXML
    public void initialize() {
        // Security check
        User user = AuthService.getCurrentUser();
        if (user == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            System.err.println("Access Denied: Unprivileged access to AdminExpiryController.");
            return;
        }

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDosage.setCellValueFactory(new PropertyValueFactory<>("dosage"));
        colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Add custom formatting / color coding for the status column
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("EXPIRED")) {
                        setTextFill(Color.web("#ef4444")); // Red
                        setStyle("-fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("EXPIRING SOON")) {
                        setTextFill(Color.web("#f59e0b")); // Orange/Yellow
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.web("#10b981")); // Green
                        setStyle("-fx-font-weight: bold;");
                    }
                }
            }
        });

        loadExpiryData();
    }

    private void loadExpiryData() {
        List<Medicine> allMeds = medicineDAO.getAll();
        List<ExpiryRow> rows = new ArrayList<>();

        for (Medicine med : allMeds) {
            String username = userDAO.getUsernameById(med.getUserId());
            String status = calculateExpiryStatus(med);
            rows.add(new ExpiryRow(
                med.getName(),
                med.getDosage(),
                med.getExpiryDate(),
                username,
                status
            ));
        }

        ObservableList<ExpiryRow> observableList = FXCollections.observableArrayList(rows);
        expiryTable.setItems(observableList);
    }

    private String calculateExpiryStatus(Medicine med) {
        if (stockService.isExpired(med)) {
            return "EXPIRED";
        }
        
        if (med.getExpiryDate() == null || med.getExpiryDate().trim().isEmpty()) {
            return "ACTIVE";
        }

        try {
            LocalDate expiry = LocalDate.parse(med.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            long daysToExpiry = ChronoUnit.DAYS.between(LocalDate.now(), expiry);
            if (daysToExpiry >= 0 && daysToExpiry <= 30) {
                return "EXPIRING SOON";
            } else if (daysToExpiry < 0) {
                return "EXPIRED";
            }
        } catch (Exception e) {
            // fallback
        }
        return "ACTIVE";
    }

    public static class ExpiryRow {
        private final String name;
        private final String dosage;
        private final String expiryDate;
        private final String username;
        private final String status;

        public ExpiryRow(String name, String dosage, String expiryDate, String username, String status) {
            this.name = name;
            this.dosage = dosage;
            this.expiryDate = expiryDate;
            this.username = username;
            this.status = status;
        }

        public String getName() { return name; }
        public String getDosage() { return dosage; }
        public String getExpiryDate() { return expiryDate; }
        public String getUsername() { return username; }
        public String getStatus() { return status; }
    }
}
