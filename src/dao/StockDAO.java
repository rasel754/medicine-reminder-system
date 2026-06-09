package dao;

import model.Stock;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * StockDAO handles CRUD and quantity adjustments on the 'stock' table in MySQL.
 */
public class StockDAO {

    public Stock addStock(Stock stock) {
        String query = "INSERT INTO stock (medicine_name, quantity) VALUES (?, ?) " +
                       "ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, stock.getMedicineName());
            pstmt.setInt(2, stock.getQuantity());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        stock.setId(rs.getInt(1));
                        return stock;
                    }
                }
            }
            // If it triggered an update on duplicate, fetch the existing record to return
            return getByMedicineName(stock.getMedicineName());
        } catch (SQLException e) {
            System.err.println("SQL Exception in StockDAO.addStock: " + e.getMessage());
        }
        return null;
    }

    public boolean updateQuantity(String medicineName, int quantity) {
        String query = "UPDATE stock SET quantity = ? WHERE medicine_name = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, quantity);
            pstmt.setString(2, medicineName);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL Exception in StockDAO.updateQuantity: " + e.getMessage());
        }
        return false;
    }

    public Stock getByMedicineName(String medicineName) {
        String query = "SELECT id, medicine_name, quantity FROM stock WHERE medicine_name = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, medicineName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Stock(
                        rs.getInt("id"),
                        rs.getString("medicine_name"),
                        rs.getInt("quantity")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in StockDAO.getByMedicineName: " + e.getMessage());
        }
        return null;
    }

    public List<Stock> getAll() {
        List<Stock> list = new ArrayList<>();
        String query = "SELECT id, medicine_name, quantity FROM stock";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new Stock(
                    rs.getInt("id"),
                    rs.getString("medicine_name"),
                    rs.getInt("quantity")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in StockDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    public boolean deleteStock(int id) {
        String query = "DELETE FROM stock WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL Exception in StockDAO.deleteStock: " + e.getMessage());
        }
        return false;
    }
}
