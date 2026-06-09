import model.*;
import dao.*;
import service.*;
import util.DBConnection;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BackendDemo is the integration test harness for the MySQL backend.
 * It initializes database tables, registers dummy users, handles login roles,
 * adjusts stock inventory, and schedules a real-time reminder to run.
 */
public class BackendDemo {
    public static void main(String[] args) {
        System.out.println("=== Starting PillSync MySQL Backend Demo ===");
        
        // 1. Initialize MySQL tables if they do not exist
        initializeMySQLTables();

        // 2. Instantiate Services
        AuthService authService = new AuthService();
        MedicineService medicineService = new MedicineService();
        StockService stockService = new StockService();
        ReminderService reminderService = new ReminderService();

        // 3. Test Registration
        System.out.println("\n--- Testing User Registration ---");
        boolean regAdmin = authService.register("admin_user", "admin123", "ADMIN");
        boolean regUser = authService.register("john_doe", "pass123", "USER");
        System.out.println("Admin registration success: " + regAdmin);
        System.out.println("User registration success: " + regUser);

        // 4. Test Login authentication
        System.out.println("\n--- Testing Login Validation ---");
        String role = authService.authenticateAndGetRole("john_doe", "pass123");
        System.out.println("Logged in user 'john_doe'. Assigned Role: " + role);

        User loggedInUser = AuthService.getCurrentUser();
        if (loggedInUser != null) {
            // 5. Test Stock addition
            System.out.println("\n--- Testing Pharmacy Inventory Tracker ---");
            boolean stockAdded1 = stockService.addOrReplenishStock("Aspirin 81mg", 100);
            boolean stockAdded2 = stockService.addOrReplenishStock("Vitamin C 500mg", 3); // low stock trigger
            System.out.println("Aspirin Stock Added: " + stockAdded1);
            System.out.println("Vitamin C Stock Added: " + stockAdded2);

            // 6. Test Expiry and low stock flags
            System.out.println("Vitamin C low stock check (threshold 5): " + stockService.checkLowStock("Vitamin C 500mg", 5));

            // 7. Test Medicine Scheduler addition
            System.out.println("\n--- Testing Medication Reminder Scheduler ---");
            String timeNow = LocalDateTime.now().plusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm"));
            System.out.println("Scheduling demo medicine for 1 minute from now: " + timeNow);

            Medicine med = new Medicine(0, "Aspirin 81mg", "1 pill", timeNow, "2027-12-31", loggedInUser.getId());
            boolean medAdded = medicineService.addMedicine(med);
            System.out.println("Medicine successfully scheduled: " + medAdded);

            // 8. Test Reminder scan thread
            System.out.println("\n--- Starting background Checker thread (runs every 60s) ---");
            reminderService.addNotificationListener((medicine, dueTime) -> {
                System.out.println("\n[ALERT NOTIFICATION] It is time to take: " + medicine.getName() + " (" + medicine.getDosage() + ") scheduled at " + dueTime);
                
                // Add usage log
                UsageLogDAO logDAO = new UsageLogDAO();
                UsageLog log = new UsageLog(0, medicine.getId(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), "TAKEN");
                logDAO.addLog(log);
                System.out.println("[LOGGED] Marked " + medicine.getName() + " as TAKEN in database usage history.");
            });
            reminderService.startChecker();

            System.out.println("Checking active alerts. Keep demo running for 70 seconds to see background notification trigger...");
            try {
                Thread.sleep(70000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            reminderService.stopChecker();
        } else {
            System.err.println("Authentication failed. Cannot run further integration tests.");
        }

        DBConnection.closeConnection();
        System.out.println("\n=== Demo Complete ===");
    }

    private static void initializeMySQLTables() {
        Connection conn = DBConnection.getConnection();
        if (conn == null) {
            System.err.println("Could not connect to MySQL server. Ensure MySQL is running on localhost:3306.");
            return;
        }

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

            System.out.println("MySQL database tables verified / initialized successfully.");
        } catch (Exception e) {
            System.err.println("Failed to initialize database tables: " + e.getMessage());
        }
    }
}
