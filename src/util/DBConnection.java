package util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DBConnection provides a reusable, single connection provider to connect
 * to the MySQL backend instance or SQLite fallback for PillSync Medicine Reminder System.
 */
public class DBConnection {
    // MySQL Database connection parameters
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/medicine_system?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "sajek@7540987";

    private static Connection connection = null;
    private static Exception lastException = null;

    /**
     * Obtains the active database connection, opening it if closed.
     * Uses a single reusable connection pooling-style pattern.
     *
     * @return Connection active JDBC connection or null if failed
     */
    public static synchronized Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            // Attempt MySQL connection
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                lastException = null; // Reset exception upon successful connection
                System.out.println("MySQL connection successfully established.");
                
                // Auto-initialize MySQL tables
                initializeMySQLTables(connection);
                
                return connection;
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("MySQL connection failed. Falling back to SQLite... Error: " + e.getMessage());
            }

            // Portable SQLite Fallback
            try {
                Class.forName("org.sqlite.JDBC");
                String sqliteUrl = "jdbc:sqlite:database/medicine_reminder.db";
                
                // Ensure parent directory exists
                File dbFile = new File("database/medicine_reminder.db");
                if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }

                connection = DriverManager.getConnection(sqliteUrl);
                lastException = null;
                System.out.println("SQLite connection successfully established.");
                
                // Auto-initialize tables
                initializeSQLiteTables(connection);
            } catch (ClassNotFoundException | SQLException ex) {
                lastException = ex;
                System.err.println("SQLite fallback connection failed: " + ex.getMessage());
            }

        } catch (SQLException e) {
            lastException = e;
            System.err.println("Failed to connect to database: " + e.getMessage());
        }
        return connection;
    }

    private static void initializeSQLiteTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "username TEXT NOT NULL UNIQUE," +
                         "password TEXT NOT NULL," +
                         "role TEXT NOT NULL" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS medicines (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "name TEXT NOT NULL," +
                         "dosage TEXT NOT NULL," +
                         "time TEXT NOT NULL," +
                         "expiry_date TEXT NOT NULL," +
                         "user_id INTEGER," +
                         "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS stock (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "medicine_name TEXT NOT NULL UNIQUE," +
                         "quantity INTEGER NOT NULL DEFAULT 0" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS usage_log (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "medicine_id INTEGER NOT NULL," +
                         "taken_date TEXT NOT NULL," +
                         "status TEXT NOT NULL," +
                         "FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS intake_logs (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "user_id INTEGER NOT NULL," +
                         "medicine_id INTEGER NOT NULL," +
                         "status TEXT NOT NULL," +
                         "date TEXT NOT NULL," +
                         "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                         "FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE" +
                         ")");

            // Seed database with default users if new
            try (Statement checkStmt = conn.createStatement();
                 java.sql.ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('john_doe', 'pass123', 'USER')");
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin_user', 'admin123', 'ADMIN')");
                    System.out.println("Default dummy users registered in SQLite database.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to initialize SQLite database tables: " + e.getMessage());
        }
    }

    private static void initializeMySQLTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "username VARCHAR(50) NOT NULL UNIQUE," +
                         "password VARCHAR(255) NOT NULL," +
                         "role VARCHAR(20) NOT NULL" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS medicines (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "name VARCHAR(100) NOT NULL," +
                         "dosage VARCHAR(50) NOT NULL," +
                         "time VARCHAR(20) NOT NULL," +
                         "expiry_date VARCHAR(20) NOT NULL," +
                         "user_id INT," +
                         "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS stock (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "medicine_name VARCHAR(100) NOT NULL UNIQUE," +
                         "quantity INT NOT NULL DEFAULT 0" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS usage_log (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "medicine_id INT NOT NULL," +
                         "taken_date VARCHAR(20) NOT NULL," +
                         "status VARCHAR(20) NOT NULL," +
                         "FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE" +
                         ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS intake_logs (" +
                         "id INT AUTO_INCREMENT PRIMARY KEY," +
                         "user_id INT NOT NULL," +
                         "medicine_id INT NOT NULL," +
                         "status VARCHAR(20) NOT NULL," +
                         "date DATE NOT NULL," +
                         "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                         "FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE CASCADE" +
                         ")");

            // Seed database with default users if new
            try (Statement checkStmt = conn.createStatement();
                 java.sql.ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('john_doe', 'pass123', 'USER')");
                    stmt.execute("INSERT INTO users (username, password, role) VALUES ('admin_user', 'admin123', 'ADMIN')");
                    System.out.println("Default dummy users registered in MySQL database.");
                }
            }
            System.out.println("MySQL database tables verified / initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Failed to initialize MySQL database tables: " + e.getMessage());
        }
    }

    /**
     * Returns the last exception encountered during getConnection(), if any.
     * Useful for diagnostics and database setup validation.
     *
     * @return the last Exception, or null if none
     */
    public static Exception getLastException() {
        return lastException;
    }

    /**
     * Closes the active connection if it is open.
     */
    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed successfully.");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}
