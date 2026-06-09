package service;

import dao.StockDAO;
import model.Medicine;
import model.Stock;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * StockService provides business capabilities for tracking pharmacy inventory limits,
 * stock replenishment, and verifying medicine expiration dates.
 */
public class StockService {
    private final StockDAO stockDAO;

    public StockService() {
        this.stockDAO = new StockDAO();
    }

    public boolean addOrReplenishStock(String medicineName, int quantity) {
        if (medicineName == null || medicineName.trim().isEmpty() || quantity < 0) {
            return false;
        }
        Stock stock = new Stock(0, medicineName, quantity);
        return stockDAO.addStock(stock) != null;
    }

    public boolean reduceStock(String medicineName, int quantity) {
        Stock existing = stockDAO.getByMedicineName(medicineName);
        if (existing != null) {
            int current = existing.getQuantity();
            if (current >= quantity) {
                return stockDAO.updateQuantity(medicineName, current - quantity);
            }
        }
        return false;
    }

    public boolean checkLowStock(String medicineName, int threshold) {
        Stock existing = stockDAO.getByMedicineName(medicineName);
        if (existing != null) {
            return existing.getQuantity() <= threshold;
        }
        return true; // If no stock record exists, it is functionally low/zero stock
    }

    /**
     * Checks if a medicine's expiration date has passed.
     *
     * @param med medicine info
     * @return true if medicine is expired, false otherwise
     */
    public boolean isExpired(Medicine med) {
        if (med.getExpiryDate() == null || med.getExpiryDate().trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate expiry = LocalDate.parse(med.getExpiryDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return LocalDate.now().isAfter(expiry);
        } catch (Exception e) {
            System.err.println("Failed to parse expiry date: " + med.getExpiryDate());
            return false;
        }
    }

    public Stock getStock(String medicineName) {
        return stockDAO.getByMedicineName(medicineName);
    }

    public List<Stock> getAllStock() {
        return stockDAO.getAll();
    }
}
