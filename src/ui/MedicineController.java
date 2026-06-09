package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import model.Medicine;
import service.AuthService;
import service.MedicineService;
import service.StockService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * MedicineController manages adding, editing, and listing medications
 * in the TableView and Form views.
 */
public class MedicineController {

    // Form fields
    @FXML
    private TextField nameField;
    @FXML
    private TextField dosageField;
    @FXML
    private TextField timeField;
    @FXML
    private DatePicker expiryDatePicker;
    @FXML
    private Label formFeedbackLabel;

    // Table view fields
    @FXML
    private TableView<Medicine> medicineTable;
    @FXML
    private TableColumn<Medicine, String> colName;
    @FXML
    private TableColumn<Medicine, String> colDosage;
    @FXML
    private TableColumn<Medicine, String> colTime;
    @FXML
    private TableColumn<Medicine, String> colExpiry;

    private final MedicineService medicineService = new MedicineService();
    private final StockService stockService = new StockService();
    
    // Track if we are editing an existing medication
    private Medicine editingMedicine = null;

    @FXML
    public void initialize() {
        if (medicineTable != null) {
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colDosage.setCellValueFactory(new PropertyValueFactory<>("dosage"));
            colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            colExpiry.setCellValueFactory(new PropertyValueFactory<>("expiryDate"));
            
            loadTableData();
        }
    }

    private void loadTableData() {
        if (AuthService.getCurrentUser() != null) {
            List<Medicine> list = medicineService.getMedicinesForUser(AuthService.getCurrentUser().getId());
            ObservableList<Medicine> observableList = FXCollections.observableArrayList(list);
            medicineTable.setItems(observableList);
        }
    }

    public void loadMedicineForEdit(Medicine med) {
        this.editingMedicine = med;
        nameField.setText(med.getName());
        dosageField.setText(med.getDosage());
        timeField.setText(med.getTime());
        if (med.getExpiryDate() != null) {
            expiryDatePicker.setValue(LocalDate.parse(med.getExpiryDate()));
        }
    }

    @FXML
    public void handleSaveMedicine(ActionEvent event) {
        String name = nameField.getText().trim();
        String dosage = dosageField.getText().trim();
        String time = timeField.getText().trim();
        LocalDate expiry = expiryDatePicker.getValue();

        if (name.isEmpty() || dosage.isEmpty() || time.isEmpty() || expiry == null) {
            formFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
            formFeedbackLabel.setText("Please fill out all fields.");
            return;
        }

        boolean success;
        if (editingMedicine != null) {
            // Edit existing
            editingMedicine.setName(name);
            editingMedicine.setDosage(dosage);
            editingMedicine.setTime(time);
            editingMedicine.setExpiryDate(expiry.toString());
            success = medicineService.updateMedicine(editingMedicine);
        } else {
            // Create new
            Medicine med = new Medicine(0, name, dosage, time, expiry.toString(), AuthService.getCurrentUser().getId());
            success = medicineService.addMedicine(med);
            // Replenish base stock level to 30 for demo
            stockService.addOrReplenishStock(name, 30);
        }

        if (success) {
            formFeedbackLabel.setStyle("-fx-text-fill: #10b981;");
            formFeedbackLabel.setText("Medication successfully saved!");
            
            // Clear fields if it was a new creation
            if (editingMedicine == null) {
                nameField.clear();
                dosageField.clear();
                timeField.clear();
                expiryDatePicker.setValue(null);
            } else {
                editingMedicine = null; // reset
            }
        } else {
            formFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
            formFeedbackLabel.setText("Failed to save medication details.");
        }
    }

    @FXML
    public void handleUpdateSelection(ActionEvent event) {
        Medicine selected = medicineTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a medication to update.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/medicine_form.fxml"));
            Parent pane = loader.load();

            // Populate form controller fields
            MedicineController formCtrl = loader.getController();
            formCtrl.loadMedicineForEdit(selected);

            // Navigate
            StackPane contentArea = (StackPane) medicineTable.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(pane);
        } catch (IOException e) {
            System.err.println("Failed to load medication form for updating: " + e.getMessage());
        }
    }

    @FXML
    public void handleDeleteSelection(ActionEvent event) {
        Medicine selected = medicineTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select a medication to delete.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this medication?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            boolean success = medicineService.deleteMedicine(selected.getId());
            if (success) {
                loadTableData();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to delete medication.", ButtonType.OK);
                err.showAndWait();
            }
        }
    }
}
