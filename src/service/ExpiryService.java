package service;

import model.Medicine;
import util.DBConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * ExpiryService provides SQL backend queries to fetch medications
 * that are expired or expiring within the next 7 days.
 */
public class ExpiryService {

    /**
     * Gets all medicines that are expired.
     */
    public List<Medicine> getExpiredMedicines() {
        List<Medicine> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        String query = "SELECT * FROM medicines WHERE expiry_date < CURDATE()";
        try {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (dbProduct != null && dbProduct.toLowerCase().contains("sqlite")) {
                query = "SELECT * FROM medicines WHERE expiry_date < date('now')";
            }
        } catch (SQLException e) {
            System.err.println("Could not read database metadata in ExpiryService.getExpiredMedicines: " + e.getMessage());
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Medicine(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("dosage"),
                    rs.getString("time"),
                    rs.getString("expiry_date"),
                    rs.getInt("user_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in ExpiryService.getExpiredMedicines: " + e.getMessage());
        }
        return list;
    }

    /**
     * Gets all medicines expiring in the next 7 days.
     */
    public List<Medicine> getNearExpiryMedicines() {
        List<Medicine> list = new ArrayList<>();
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        String query = "SELECT * FROM medicines WHERE expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY)";
        try {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (dbProduct != null && dbProduct.toLowerCase().contains("sqlite")) {
                query = "SELECT * FROM medicines WHERE expiry_date BETWEEN date('now') AND date('now', '+7 days')";
            }
        } catch (SQLException e) {
            System.err.println("Could not read database metadata in ExpiryService.getNearExpiryMedicines: " + e.getMessage());
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Medicine(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("dosage"),
                    rs.getString("time"),
                    rs.getString("expiry_date"),
                    rs.getInt("user_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in ExpiryService.getNearExpiryMedicines: " + e.getMessage());
        }
        return list;
    }
}
