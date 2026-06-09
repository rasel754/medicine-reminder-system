package ui;

import dao.UsageLogDAO;
import model.Medicine;
import model.UsageLog;
import service.ReminderService;
import service.StockService;
import util.DBConnection;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MainApp is the JavaFX entry bootstrapper. It sets up the stage, loads the CSS,
 * and boots the periodic reminder loop.
 */
public class MainApp extends Application {

    private static ReminderService reminderService;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load login FXML layout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.setTitle("PillSync - Login");
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.show();

            // Setup and start background reminders
            reminderService = new ReminderService();
            reminderService.addNotificationListener((medicine, dueTime) -> {
                Platform.runLater(() -> showReminderPopup(medicine, dueTime));
            });
            reminderService.startChecker();

        } catch (IOException e) {
            System.err.println("Critical Error: Failed to start PillSync application UI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showReminderPopup(Medicine medicine, String dueTime) {
        Stage popupStage = new Stage();
        popupStage.setTitle("Medication Alert!");
        popupStage.initStyle(StageStyle.UNDECORATED); // Clean borderless layout

        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e293b; -fx-border-color: #6366f1; -fx-border-width: 2.0; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label title = new Label("Time to take your medication!");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        Label medName = new Label(medicine.getName());
        medName.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label medDetails = new Label("Dosage: " + medicine.getDosage() + " at " + dueTime);
        medDetails.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        HBox buttons = new HBox(15);
        buttons.setAlignment(Pos.CENTER);

        Button btnTaken = new Button("Mark as Taken");
        btnTaken.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        btnTaken.setOnAction(e -> {
            // Log taken status in database
            UsageLogDAO logDAO = new UsageLogDAO();
            UsageLog log = new UsageLog(0, medicine.getId(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), "TAKEN");
            logDAO.addLog(log);
            
            // Deduct stock levels by 1
            StockService stockService = new StockService();
            stockService.reduceStock(medicine.getName(), 1);
            
            popupStage.close();
        });

        Button btnSnooze = new Button("Snooze");
        btnSnooze.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 6;");
        btnSnooze.setOnAction(e -> {
            // Pause for 5 minutes and re-trigger
            PauseTransition pause = new PauseTransition(Duration.minutes(5));
            pause.setOnFinished(evt -> Platform.runLater(() -> showReminderPopup(medicine, dueTime + " (Snoozed)")));
            pause.play();
            popupStage.close();
        });

        buttons.getChildren().addAll(btnTaken, btnSnooze);
        root.getChildren().addAll(title, medName, medDetails, buttons);

        Scene scene = new Scene(root, 360, 200);
        popupStage.setScene(scene);
        popupStage.setAlwaysOnTop(true);
        popupStage.centerOnScreen();
        popupStage.show();
    }

    @Override
    public void stop() {
        if (reminderService != null) {
            reminderService.stopChecker();
        }
        DBConnection.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
