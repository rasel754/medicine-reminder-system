package ui;

import dao.UsageLogDAO;
import dao.MedicineDAO;
import model.Medicine;
import model.UsageLog;
import service.AuthService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CalendarController builds the calendar grid, highlights taken/missed dates,
 * and loads adherence details on day click.
 */
public class CalendarController {

    @FXML
    private Label monthTitleLabel;

    @FXML
    private GridPane calendarGrid;

    @FXML
    private Label selectedDateLabel;

    @FXML
    private ListView<String> logsListView;

    private LocalDate activeMonth;
    private final UsageLogDAO usageLogDAO = new UsageLogDAO();
    private final MedicineDAO medicineDAO = new MedicineDAO();

    @FXML
    public void initialize() {
        activeMonth = LocalDate.now().withDayOfMonth(1);
        loadCalendar();
    }

    private void loadCalendar() {
        calendarGrid.getChildren().clear();
        logsListView.getItems().clear();
        selectedDateLabel.setText("Click a date to see logs");

        int year = activeMonth.getYear();
        int month = activeMonth.getMonthValue();

        // Update Month Title
        String monthName = activeMonth.getMonth().name();
        monthTitleLabel.setText(monthName.substring(0, 1) + monthName.substring(1).toLowerCase() + " " + year);

        // Fetch logs for current user to map to dates
        Map<String, List<UsageLog>> logsMap = new HashMap<>();
        if (AuthService.getCurrentUser() != null) {
            List<UsageLog> logs = usageLogDAO.getAll(); // get all logs and filter in memory for this user's medicines
            for (UsageLog log : logs) {
                if (log.getTakenDate().length() >= 10) {
                    String dateKey = log.getTakenDate().substring(0, 10);
                    logsMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(log);
                }
            }
        }

        // Draw weekday headers Sun-Sat
        String[] weekdays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < weekdays.length; i++) {
            Label header = new Label(weekdays[i]);
            header.setStyle("-fx-font-weight: bold; -fx-text-fill: #94a3b8;");
            GridPane.setConstraints(header, i, 0);
            calendarGrid.getChildren().add(header);
        }

        YearMonth ym = YearMonth.of(year, month);
        int length = ym.lengthOfMonth();
        int startDayOfWeek = activeMonth.getDayOfWeek().getValue() % 7; // 0=Sun, 1=Mon, ..., 6=Sat

        // Load day cells
        int gridRow = 1;
        int gridCol = startDayOfWeek;

        for (int day = 1; day <= length; day++) {
            LocalDate dayDate = LocalDate.of(year, month, day);
            String dateKey = dayDate.toString();

            VBox cell = new VBox(5);
            cell.setAlignment(Pos.CENTER);
            cell.setPrefSize(60, 50);
            cell.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 6; -fx-cursor: hand;");
            
            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            cell.getChildren().add(dayNum);

            List<UsageLog> dayLogs = logsMap.get(dateKey);
            if (dayLogs != null && !dayLogs.isEmpty()) {
                boolean hasSkipped = false;
                for (UsageLog l : dayLogs) {
                    if ("SKIPPED".equalsIgnoreCase(l.getStatus())) {
                        hasSkipped = true;
                        break;
                    }
                }
                
                Circle dot = new Circle(4);
                if (hasSkipped) {
                    dot.setStyle("-fx-fill: #f59e0b;"); // Yellow for skipped
                } else {
                    dot.setStyle("-fx-fill: #10b981;"); // Green for taken
                }
                cell.getChildren().add(dot);
            }

            // Click action
            cell.setOnMouseClicked(e -> showLogsForDate(dayDate, dayLogs));

            GridPane.setConstraints(cell, gridCol, gridRow);
            calendarGrid.getChildren().add(cell);

            gridCol++;
            if (gridCol > 6) {
                gridCol = 0;
                gridRow++;
            }
        }
    }

    private void showLogsForDate(LocalDate date, List<UsageLog> dayLogs) {
        selectedDateLabel.setText("Logs for " + date.toString());
        logsListView.getItems().clear();

        if (dayLogs == null || dayLogs.isEmpty()) {
            logsListView.getItems().add("No adherence logs registered.");
            return;
        }

        for (UsageLog log : dayLogs) {
            Medicine med = medicineDAO.getById(log.getMedicineId());
            String name = med != null ? med.getName() : "Med #" + log.getMedicineId();
            String time = log.getTakenDate().length() >= 16 ? log.getTakenDate().substring(11, 16) : "";
            
            logsListView.getItems().add(
                name + " (" + log.getStatus() + ") at " + time
            );
        }
    }

    @FXML
    public void handlePrevMonth(ActionEvent event) {
        activeMonth = activeMonth.minusMonths(1);
        loadCalendar();
    }

    @FXML
    public void handleNextMonth(ActionEvent event) {
        activeMonth = activeMonth.plusMonths(1);
        loadCalendar();
    }
}
