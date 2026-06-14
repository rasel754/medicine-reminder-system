package ui;

import dao.MedicineDAO;
import dao.UserDAO;
import model.Medicine;
import service.AuthService;
import service.ExpiryService;
import model.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

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

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private Button btnSort;

    @FXML
    private VBox urgentBanner;

    @FXML
    private Label urgentBannerText;

    private final MedicineDAO medicineDAO = new MedicineDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ExpiryService expiryService = new ExpiryService();

    private static final PseudoClass EXPIRED_PSEUDO = PseudoClass.getPseudoClass("expired");
    private static final PseudoClass EXPIRING_SOON_PSEUDO = PseudoClass.getPseudoClass("expiring-soon");
    private static final PseudoClass SAFE_PSEUDO = PseudoClass.getPseudoClass("safe");

    private boolean sortAscending = true;

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

        // Add custom row styling using PseudoClass
        expiryTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ExpiryRow item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    pseudoClassStateChanged(EXPIRED_PSEUDO, false);
                    pseudoClassStateChanged(EXPIRING_SOON_PSEUDO, false);
                    pseudoClassStateChanged(SAFE_PSEUDO, false);
                } else {
                    String status = item.getStatus();
                    pseudoClassStateChanged(EXPIRED_PSEUDO, status.contains("Expired"));
                    pseudoClassStateChanged(EXPIRING_SOON_PSEUDO, status.contains("Expiring Soon"));
                    pseudoClassStateChanged(SAFE_PSEUDO, status.contains("Safe"));
                }
            }
        });

        // Initialize Filter ComboBox
        filterComboBox.setItems(FXCollections.observableArrayList("All", "Today", "7 Days"));
        filterComboBox.setValue("All");
        filterComboBox.setOnAction(e -> filterAndLoadData());

        filterAndLoadData();
    }

    private void filterAndLoadData() {
        String filter = filterComboBox.getValue();
        List<Medicine> medsToProcess = new ArrayList<>();
        
        if ("Today".equalsIgnoreCase(filter)) {
            // Expired or expiring today
            medsToProcess = expiryService.getExpiredMedicines();
        } else if ("7 Days".equalsIgnoreCase(filter)) {
            // Expired + expiring in 7 days
            medsToProcess.addAll(expiryService.getExpiredMedicines());
            medsToProcess.addAll(expiryService.getNearExpiryMedicines());
        } else {
            // All
            medsToProcess = medicineDAO.getAll();
        }

        List<ExpiryRow> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Medicine med : medsToProcess) {
            String username = userDAO.getUsernameById(med.getUserId());
            String status = calculateExpiryStatus(med);

            // Extra safety filter for "Today" option in case database queries have overlaps
            if ("Today".equalsIgnoreCase(filter)) {
                if (med.getExpiryDate() != null && !med.getExpiryDate().trim().isEmpty()) {
                    try {
                        LocalDate expiry = LocalDate.parse(med.getExpiryDate());
                        if (expiry.isAfter(today)) {
                            continue; // Only expired/expiring today
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            rows.add(new ExpiryRow(
                med.getName(),
                med.getDosage(),
                med.getExpiryDate(),
                username,
                status
            ));
        }

        // Apply sorting
        sortRows(rows);

        ObservableList<ExpiryRow> observableList = FXCollections.observableArrayList(rows);
        expiryTable.setItems(observableList);

        // Update urgent banner
        updateUrgentBanner(rows);
    }

    private String calculateExpiryStatus(Medicine med) {
        if (med.getExpiryDate() == null || med.getExpiryDate().trim().isEmpty()) {
            return "Safe ✅";
        }

        try {
            LocalDate expiry = LocalDate.parse(med.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate today = LocalDate.now();
            if (expiry.isBefore(today)) {
                return "Expired ❌";
            }
            long daysToExpiry = ChronoUnit.DAYS.between(today, expiry);
            if (daysToExpiry >= 0 && daysToExpiry <= 7) {
                return "Expiring Soon ⚠️";
            }
        } catch (Exception e) {
            // fallback
        }
        return "Safe ✅";
    }

    private void sortRows(List<ExpiryRow> rows) {
        rows.sort((r1, r2) -> {
            String d1 = r1.getExpiryDate();
            String d2 = r2.getExpiryDate();
            if (d1 == null || d1.trim().isEmpty()) return 1;
            if (d2 == null || d2.trim().isEmpty()) return -1;
            try {
                LocalDate date1 = LocalDate.parse(d1);
                LocalDate date2 = LocalDate.parse(d2);
                int comp = date1.compareTo(date2);
                return sortAscending ? comp : -comp;
            } catch (Exception e) {
                return 1;
            }
        });
    }

    @FXML
    public void handleSort() {
        sortAscending = !sortAscending;
        btnSort.setText(sortAscending ? "Sort: Expiry Date (Asc)" : "Sort: Expiry Date (Desc)");
        filterAndLoadData();
    }

    private void updateUrgentBanner(List<ExpiryRow> rows) {
        long expiredCount = rows.stream().filter(r -> r.getStatus().contains("Expired")).count();
        long soonCount = rows.stream().filter(r -> r.getStatus().contains("Expiring Soon")).count();

        if (expiredCount > 0 || soonCount > 0) {
            urgentBanner.setVisible(true);
            urgentBanner.setManaged(true);

            // Find the top urgent row (earliest date)
            ExpiryRow mostUrgent = rows.stream()
                .filter(r -> r.getStatus().contains("Expired") || r.getStatus().contains("Expiring Soon"))
                .findFirst()
                .orElse(null);

            StringBuilder msg = new StringBuilder();
            if (expiredCount > 0) {
                msg.append("⚠️ ").append(expiredCount).append(" Expired medicine(s). ");
            }
            if (soonCount > 0) {
                msg.append("⚠️ ").append(soonCount).append(" Expiring soon. ");
            }
            if (mostUrgent != null) {
                msg.append("Top Urgent: ").append(mostUrgent.getName())
                   .append(" (Expires: ").append(mostUrgent.getExpiryDate()).append(")");
            }
            urgentBannerText.setText(msg.toString());

            // Red alerts for Expired, Yellow/Amber alerts for Expiring Soon
            if (expiredCount > 0) {
                urgentBanner.setStyle("-fx-background-color: #3b1515; -fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15;");
                urgentBannerText.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");
            } else {
                urgentBanner.setStyle("-fx-background-color: #3c2f0f; -fx-border-color: #fbbf24; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 15;");
                urgentBannerText.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");
            }
        } else {
            urgentBanner.setVisible(false);
            urgentBanner.setManaged(false);
        }
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
