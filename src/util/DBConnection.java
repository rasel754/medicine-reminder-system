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

            // Seed database with test medicines if empty
            try (Statement checkMedsStmt = conn.createStatement();
                 java.sql.ResultSet rsMeds = checkMedsStmt.executeQuery("SELECT COUNT(*) FROM medicines")) {
                if (rsMeds.next() && rsMeds.getInt(1) == 0) {
                    int userId = 1;
                    try (java.sql.ResultSet rsUser = checkMedsStmt.executeQuery("SELECT id FROM users LIMIT 1")) {
                        if (rsUser.next()) {
                            userId = rsUser.getInt(1);
                        }
                    }
                    String pastDate = java.time.LocalDate.now().minusDays(5).toString();
                    String nearDate = java.time.LocalDate.now().plusDays(3).toString();
                    String futureDate = java.time.LocalDate.now().plusMonths(6).toString();
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Amoxicillin 500mg', '1 capsule', '08:00', '" + pastDate + "', " + userId + ")");
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Ibuprofen 400mg', '1 tablet', '13:00', '" + nearDate + "', " + userId + ")");
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Loratadine 10mg', '1 tablet', '20:00', '" + futureDate + "', " + userId + ")");
                    System.out.println("Test medicines with past, near, and future expiry dates seeded in SQLite database.");
                }
            }

            // Seed database with default stock if empty
            try (Statement checkStockStmt = conn.createStatement();
                 java.sql.ResultSet rsStock = checkStockStmt.executeQuery("SELECT COUNT(*) FROM stock")) {
                if (rsStock.next() && rsStock.getInt(1) == 0) {
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Amoxicillin 500mg', 2)");
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Ibuprofen 400mg', 15)");
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Loratadine 10mg', 40)");
                    System.out.println("Default stock seeded in SQLite database.");
                }
            }

            // Seed database with test intake logs if empty
            try (Statement checkLogsStmt = conn.createStatement();
                 java.sql.ResultSet rsLogs = checkLogsStmt.executeQuery("SELECT COUNT(*) FROM intake_logs")) {
                if (rsLogs.next() && rsLogs.getInt(1) == 0) {
                    int userId = 1;
                    java.util.List<Integer> medIds = new java.util.ArrayList<>();
                    try (java.sql.ResultSet rsUser = checkLogsStmt.executeQuery("SELECT id FROM users LIMIT 1")) {
                        if (rsUser.next()) {
                            userId = rsUser.getInt(1);
                        }
                    }
                    try (java.sql.ResultSet rsMeds = checkLogsStmt.executeQuery("SELECT id FROM medicines")) {
                        while (rsMeds.next()) {
                            medIds.add(rsMeds.getInt("id"));
                        }
                    }
                    if (!medIds.isEmpty()) {
                        int medId1 = medIds.get(0);
                        int medId2 = medIds.size() > 1 ? medIds.get(1) : medId1;
                        int medId3 = medIds.size() > 2 ? medIds.get(2) : medId2;
                        
                        String d1 = java.time.LocalDate.now().minusDays(3).toString();
                        String d2 = java.time.LocalDate.now().minusDays(2).toString();
                        String d3 = java.time.LocalDate.now().minusDays(1).toString();
                        String dToday = java.time.LocalDate.now().toString();

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d1));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId2, d1));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId3, d1));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d2));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId2, d2));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId3, d2));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d3));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId2, d3));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId3, d3));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId1, dToday));
                        System.out.println("Test intake logs seeded in SQLite database.");
                    }
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

            // Seed database with test medicines if empty
            try (Statement checkMedsStmt = conn.createStatement();
                 java.sql.ResultSet rsMeds = checkMedsStmt.executeQuery("SELECT COUNT(*) FROM medicines")) {
                if (rsMeds.next() && rsMeds.getInt(1) == 0) {
                    int userId = 1;
                    try (java.sql.ResultSet rsUser = checkMedsStmt.executeQuery("SELECT id FROM users LIMIT 1")) {
                        if (rsUser.next()) {
                            userId = rsUser.getInt(1);
                        }
                    }
                    String pastDate = java.time.LocalDate.now().minusDays(5).toString();
                    String nearDate = java.time.LocalDate.now().plusDays(3).toString();
                    String futureDate = java.time.LocalDate.now().plusMonths(6).toString();
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Amoxicillin 500mg', '1 capsule', '08:00', '" + pastDate + "', " + userId + ")");
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Ibuprofen 400mg', '1 tablet', '13:00', '" + nearDate + "', " + userId + ")");
                    stmt.execute("INSERT INTO medicines (name, dosage, time, expiry_date, user_id) VALUES ('Loratadine 10mg', '1 tablet', '20:00', '" + futureDate + "', " + userId + ")");
                    System.out.println("Test medicines with past, near, and future expiry dates seeded in MySQL database.");
                }
            }

            // Seed database with default stock if empty
            try (Statement checkStockStmt = conn.createStatement();
                 java.sql.ResultSet rsStock = checkStockStmt.executeQuery("SELECT COUNT(*) FROM stock")) {
                if (rsStock.next() && rsStock.getInt(1) == 0) {
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Amoxicillin 500mg', 2)");
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Ibuprofen 400mg', 15)");
                    stmt.execute("INSERT INTO stock (medicine_name, quantity) VALUES ('Loratadine 10mg', 40)");
                    System.out.println("Default stock seeded in MySQL database.");
                }
            }

            // Seed database with test intake logs if empty
            try (Statement checkLogsStmt = conn.createStatement();
                 java.sql.ResultSet rsLogs = checkLogsStmt.executeQuery("SELECT COUNT(*) FROM intake_logs")) {
                if (rsLogs.next() && rsLogs.getInt(1) == 0) {
                    int userId = 1;
                    java.util.List<Integer> medIds = new java.util.ArrayList<>();
                    try (java.sql.ResultSet rsUser = checkLogsStmt.executeQuery("SELECT id FROM users LIMIT 1")) {
                        if (rsUser.next()) {
                            userId = rsUser.getInt(1);
                        }
                    }
                    try (java.sql.ResultSet rsMeds = checkLogsStmt.executeQuery("SELECT id FROM medicines")) {
                        while (rsMeds.next()) {
                            medIds.add(rsMeds.getInt("id"));
                        }
                    }
                    if (!medIds.isEmpty()) {
                        int medId1 = medIds.get(0);
                        int medId2 = medIds.size() > 1 ? medIds.get(1) : medId1;
                        int medId3 = medIds.size() > 2 ? medIds.get(2) : medId2;
                        
                        String d1 = java.time.LocalDate.now().minusDays(3).toString();
                        String d2 = java.time.LocalDate.now().minusDays(2).toString();
                        String d3 = java.time.LocalDate.now().minusDays(1).toString();
                        String dToday = java.time.LocalDate.now().toString();

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d1));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId2, d1));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId3, d1));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d2));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId2, d2));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId3, d2));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId1, d3));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId2, d3));
                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'taken', '%s')", userId, medId3, d3));

                        stmt.execute(String.format("INSERT INTO intake_logs (user_id, medicine_id, status, date) VALUES (%d, %d, 'missed', '%s')", userId, medId1, dToday));
                        System.out.println("Test intake logs seeded in MySQL database.");
                    }
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
