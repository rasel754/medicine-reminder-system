package ui;

import model.UsageDTO;
import model.User;
import service.AuthService;
import service.UsageService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.List;

public class AdminUsageController {

    @FXML
    private ComboBox<String> filterComboBox;

    @FXML
    private Label lblTotalDoses;

    @FXML
    private Label lblAdherenceRate;

    @FXML
    private Label lblTopMedicine;

    @FXML
    private TableView<UsageDTO> mostUsedTable;

    @FXML
    private TableColumn<UsageDTO, String> colMedicineName;

    @FXML
    private TableColumn<UsageDTO, Integer> colUsageCount;

    @FXML
    private BarChart<String, Number> dailyBarChart;

    @FXML
    private CategoryAxis dailyXAxis;

    @FXML
    private LineChart<String, Number> weeklyLineChart;

    @FXML
    private CategoryAxis weeklyXAxis;

    private final UsageService usageService = new UsageService();

    @FXML
    public void initialize() {
        // Security check
        User user = AuthService.getCurrentUser();
        if (user == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            System.err.println("Access Denied: Unprivileged access to AdminUsageController.");
            return;
        }

        // Configure ComboBox filters
        filterComboBox.setItems(FXCollections.observableArrayList("Today", "Last 7 days", "All time"));
        filterComboBox.setValue("All time");
        filterComboBox.setOnAction(e -> loadAnalyticsData());

        // Configure Table Columns
        colMedicineName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colUsageCount.setCellValueFactory(new PropertyValueFactory<>("count"));

        // Add custom cell factory to highlight top 5 medicines
        colMedicineName.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    int index = getIndex();
                    if (index == 0) {
                        setTextFill(Color.web("#10b981")); // Emerald Green for Rank 1
                        setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    } else if (index < 5) {
                        setTextFill(Color.web("#6366f1")); // Indigo for Rank 2-5
                        setStyle("-fx-font-weight: bold;");
                    } else {
                        setTextFill(Color.web("#f8fafc")); // Standard slate
                        setStyle("-fx-font-weight: normal;");
                    }
                }
            }
        });

        // Initialize and load analytics
        loadAnalyticsData();
    }

    private void loadAnalyticsData() {
        String filter = filterComboBox.getValue();
        System.out.println("Loading Admin analytics data with filter: " + filter);

        // 1. Fetch statistics counts
        int takenCount = usageService.getTakenCount(filter);
        int totalCount = usageService.getTotalCount(filter);
        lblTotalDoses.setText(String.valueOf(takenCount));

        // Compute adherence rate %
        double adherenceRate = 0.0;
        if (totalCount > 0) {
            adherenceRate = ((double) takenCount / totalCount) * 100.0;
        }
        lblAdherenceRate.setText(String.format("%.1f%%", adherenceRate));

        // 2. Load Most Used Medicines Table
        List<UsageDTO> mostUsed = usageService.getMostUsedMedicines(filter);
        ObservableList<UsageDTO> tableData = FXCollections.observableArrayList(mostUsed);
        mostUsedTable.setItems(tableData);

        // Highlight top #1 medicine in the KPI card
        if (!mostUsed.isEmpty()) {
            UsageDTO topMed = mostUsed.get(0);
            lblTopMedicine.setText(topMed.getName() + " (" + topMed.getCount() + ")");
            lblTopMedicine.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #10b981; -fx-padding: 8 0 0 0;");
        } else {
            lblTopMedicine.setText("None");
            lblTopMedicine.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-padding: 8 0 0 0;");
        }

        // 3. Load Daily Usage BarChart
        dailyBarChart.getData().clear();
        List<UsageDTO> dailyData = usageService.getDailyUsage(filter);
        XYChart.Series<String, Number> dailySeries = new XYChart.Series<>();
        for (UsageDTO dto : dailyData) {
            dailySeries.getData().add(new XYChart.Data<>(dto.getName(), dto.getCount()));
        }
        dailyBarChart.getData().add(dailySeries);

        // 4. Load Weekly Usage LineChart
        weeklyLineChart.getData().clear();
        List<UsageDTO> weeklyData = usageService.getWeeklyUsage(filter);
        XYChart.Series<String, Number> weeklySeries = new XYChart.Series<>();
        for (UsageDTO dto : weeklyData) {
            weeklySeries.getData().add(new XYChart.Data<>(dto.getName(), dto.getCount()));
        }
        weeklyLineChart.getData().add(weeklySeries);
    }
}
