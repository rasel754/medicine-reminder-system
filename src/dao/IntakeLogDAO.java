package dao;

import util.DBConnection;
import model.IntakeLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IntakeLogDAO manages CRUD operations on the 'intake_logs' table in SQLite/MySQL.
 */
public class IntakeLogDAO {

    /**
     * Inserts a daily intake log entry for a user and medicine with status 'taken' or 'missed' for today.
     *
     * @param userId     id of the user
     * @param medicineId id of the medicine
     * @param status     status string ('taken' or 'missed')
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertLog(int userId, int medicineId, String status) {
        String query = "INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (?, ?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("Database Connection is null in IntakeLogDAO.insertLog");
            return false;
        }

        String today = LocalDate.now().toString(); // YYYY-MM-DD
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, medicineId);
            pstmt.setString(3, status);
            pstmt.setString(4, today);

            int affectedRows = pstmt.executeUpdate();
            System.out.println("SQL Execution (insertLog) - Query: " + query + " | Result: " + affectedRows + " row(s) affected.");
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("SQL Exception in IntakeLogDAO.insertLog: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if an intake log already exists for a user's medicine on a given date.
     *
     * @param userId     id of the user
     * @param medicineId id of the medicine
     * @param date       date string formatted as YYYY-MM-DD
     * @return true if an entry exists, false otherwise
     */
    public boolean checkExists(int userId, int medicineId, String date) {
        String query = "SELECT COUNT(*) FROM intake_logs WHERE user_id = ? AND medicine_id = ? AND date = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, medicineId);
            pstmt.setString(3, date);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    boolean exists = rs.getInt(1) > 0;
                    System.out.println("SQL Execution (checkExists) - Query: " + query + " [Parameters: user=" + userId + ", medicine=" + medicineId + ", date=" + date + "] | Result: " + exists);
                    return exists;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in IntakeLogDAO.checkExists: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves all medication intake logs for a user, including medicine name and dosage.
     *
     * @param userId id of the user
     * @return List of IntakeLogs with populated medicine details, ordered by date descending
     */
    public List<IntakeLog> getLogsByUserId(int userId) {
        List<IntakeLog> list = new ArrayList<>();
        String query = "SELECT il.id, il.user_id, il.medicine_id, m.name AS medicine_name, m.dosage AS medicine_dosage, il.status, il.date " +
                       "FROM intake_logs il " +
                       "JOIN medicines m ON il.medicine_id = m.id " +
                       "WHERE il.user_id = ? " +
                       "ORDER BY il.date DESC, il.id DESC";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return list;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    IntakeLog log = new IntakeLog(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("medicine_id"),
                        rs.getString("status"),
                        rs.getString("date")
                    );
                    log.setMedicineName(rs.getString("medicine_name"));
                    log.setMedicineDosage(rs.getString("medicine_dosage"));
                    list.add(log);
                }
            }
            System.out.println("SQL Execution (getLogsByUserId) - Query: " + query + " [User: " + userId + "] | Result: " + list.size() + " logs retrieved.");
        } catch (SQLException e) {
            System.err.println("SQL Exception in IntakeLogDAO.getLogsByUserId: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }
}
