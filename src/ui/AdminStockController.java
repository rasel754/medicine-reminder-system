package ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Stock;
import service.StockService;
import service.AuthService;
import model.User;

import java.util.List;

public class AdminStockController {

    @FXML
    private TableView<Stock> stockTable;

    @FXML
    private TableColumn<Stock, String> colMedicineName;

    @FXML
    private TableColumn<Stock, Integer> colQuantity;

    @FXML
    private TextField stockNameField;

    @FXML
    private TextField stockQuantityField;

    @FXML
    private Label stockFeedbackLabel;

    private final StockService stockService = new StockService();

    @FXML
    public void initialize() {
        // Enforce role security: Check if admin is logged in
        User user = AuthService.getCurrentUser();
        if (user == null || !user.getRole().equalsIgnoreCase("ADMIN")) {
            System.err.println("Access Denied: Unprivileged access to AdminStockController.");
            return;
        }

        colMedicineName.setCellValueFactory(new PropertyValueFactory<>("medicineName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        loadStockData();

        // Add selection listener to table
        stockTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                stockNameField.setText(newSelection.getMedicineName());
                stockQuantityField.setText(String.valueOf(newSelection.getQuantity()));
            }
        });
    }

    private void loadStockData() {
        List<Stock> list = stockService.getAllStock();
        ObservableList<Stock> observableList = FXCollections.observableArrayList(list);
        stockTable.setItems(observableList);
    }

    @FXML
    public void handleUpdateStock(ActionEvent event) {
        String name = stockNameField.getText().trim();
        String qtyText = stockQuantityField.getText().trim();

        if (name.isEmpty() || qtyText.isEmpty()) {
            stockFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
            stockFeedbackLabel.setText("Please fill out all fields.");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
            if (quantity < 0) {
                stockFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
                stockFeedbackLabel.setText("Quantity must be a positive number.");
                return;
            }
        } catch (NumberFormatException e) {
            stockFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
            stockFeedbackLabel.setText("Invalid quantity value.");
            return;
        }

        // We check if the stock already exists
        Stock existing = stockService.getStock(name);
        boolean success;
        if (existing != null) {
            // Update quantity to new value directly
            dao.StockDAO dao = new dao.StockDAO();
            success = dao.updateQuantity(name, quantity);
        } else {
            // Add new stock
            success = stockService.addOrReplenishStock(name, quantity);
        }

        if (success) {
            stockFeedbackLabel.setStyle("-fx-text-fill: #10b981;");
            stockFeedbackLabel.setText("Stock successfully updated!");
            stockNameField.clear();
            stockQuantityField.clear();
            loadStockData();
        } else {
            stockFeedbackLabel.setStyle("-fx-text-fill: #ef4444;");
            stockFeedbackLabel.setText("Failed to update stock in database.");
        }
    }

    @FXML
    public void handleDeleteStock(ActionEvent event) {
        Stock selected = stockTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please select an inventory item to delete.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this stock product?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
        if (confirm.getResult() == ButtonType.YES) {
            dao.StockDAO dao = new dao.StockDAO();
            boolean success = dao.deleteStock(selected.getId());
            if (success) {
                stockNameField.clear();
                stockQuantityField.clear();
                loadStockData();
            } else {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to delete stock entry.", ButtonType.OK);
                err.showAndWait();
            }
        }
    }
}
