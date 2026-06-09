package dao;

import model.UsageLog;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * UsageLogDAO handles records of the daily medication reminder history.
 */
public class UsageLogDAO {

    public UsageLog addLog(UsageLog log) {
        String query = "INSERT INTO usage_log (medicine_id, taken_date, status) VALUES (?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, log.getMedicineId());
            pstmt.setString(2, log.getTakenDate());
            pstmt.setString(3, log.getStatus());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        log.setId(rs.getInt(1));
                        return log;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UsageLogDAO.addLog: " + e.getMessage());
        }
        return null;
    }

    public List<UsageLog> getLogsByMedicineId(int medicineId) {
        List<UsageLog> list = new ArrayList<>();
        String query = "SELECT id, medicine_id, taken_date, status FROM usage_log WHERE medicine_id = ? ORDER BY taken_date DESC";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, medicineId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new UsageLog(
                        rs.getInt("id"),
                        rs.getInt("medicine_id"),
                        rs.getString("taken_date"),
                        rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UsageLogDAO.getLogsByMedicineId: " + e.getMessage());
        }
        return list;
    }

    public List<UsageLog> getAll() {
        List<UsageLog> list = new ArrayList<>();
        String query = "SELECT id, medicine_id, taken_date, status FROM usage_log ORDER BY taken_date DESC";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                list.add(new UsageLog(
                    rs.getInt("id"),
                    rs.getInt("medicine_id"),
                    rs.getString("taken_date"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UsageLogDAO.getAll: " + e.getMessage());
        }
        return list;
    }
}
