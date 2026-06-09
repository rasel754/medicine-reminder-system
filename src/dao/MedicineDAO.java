package dao;

import model.Medicine;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * MedicineDAO handles CRUD operations on the 'medicines' table in MySQL.
 */
public class MedicineDAO {

    public Medicine add(Medicine med) {
        String query = "INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, med.getName());
            pstmt.setString(2, med.getDosage());
            pstmt.setString(3, med.getTime());
            pstmt.setString(4, med.getExpiryDate());
            pstmt.setInt(5, med.getUserId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        med.setId(rs.getInt(1));
                        return med;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in MedicineDAO.add: " + e.getMessage());
        }
        return null;
    }

    public boolean update(Medicine med) {
        String query = "UPDATE medicines SET name = ?, dosage = ?, time = ?, expiry_date = ?, user_id = ? WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, med.getName());
            pstmt.setString(2, med.getDosage());
            pstmt.setString(3, med.getTime());
            pstmt.setString(4, med.getExpiryDate());
            pstmt.setInt(5, med.getUserId());
            pstmt.setInt(6, med.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL Exception in MedicineDAO.update: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int id) {
        String query = "DELETE FROM medicines WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SQL Exception in MedicineDAO.delete: " + e.getMessage());
        }
        return false;
    }

    public List<Medicine> getAll() {
        List<Medicine> list = new ArrayList<>();
        String query = "SELECT id, name, dosage, time, expiry_date, user_id FROM medicines";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
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
            System.err.println("SQL Exception in MedicineDAO.getAll: " + e.getMessage());
        }
        return list;
    }

    public List<Medicine> getByUserId(int userId) {
        List<Medicine> list = new ArrayList<>();
        String query = "SELECT id, name, dosage, time, expiry_date, user_id FROM medicines WHERE user_id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in MedicineDAO.getByUserId: " + e.getMessage());
        }
        return list;
    }

    public Medicine getById(int id) {
        String query = "SELECT id, name, dosage, time, expiry_date, user_id FROM medicines WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Medicine(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("dosage"),
                        rs.getString("time"),
                        rs.getString("expiry_date"),
                        rs.getInt("user_id")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in MedicineDAO.getById: " + e.getMessage());
        }
        return null;
    }
}
