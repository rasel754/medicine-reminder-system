package dao;

import model.User;
import util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * UserDAO handles database authentication and registration queries for PillSync users.
 */
public class UserDAO {

    /**
     * Authenticates a user with username and password.
     *
     * @param username user name
     * @param password user password
     * @return User object if authenticated, null otherwise
     */
    public User login(String username, String password) {
        String query = "SELECT id, username, password, role FROM users WHERE username = ? AND password = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("Database connection not active.");
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UserDAO.login: " + e.getMessage());
        }
        return null;
    }

    /**
     * Registers a new user inside the database.
     *
     * @param user user details
     * @return registered User with DB populated ID
     */
    public User registerUser(User user) {
        String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return null;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UserDAO.registerUser: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if a username is already taken in the database.
     *
     * @param username user name to check
     * @return true if username exists, false otherwise
     */
    public boolean usernameExists(String username) {
        String query = "SELECT COUNT(*) FROM users WHERE username = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return false;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UserDAO.usernameExists: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves username by ID.
     */
    public String getUsernameById(int id) {
        String query = "SELECT username FROM users WHERE id = ?";
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            return "Unknown";
        }
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in UserDAO.getUsernameById: " + e.getMessage());
        }
        return "Unknown";
    }
}

