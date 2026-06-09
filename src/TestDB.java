import util.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class TestDB {
    public static void main(String[] args) {
        System.out.println("Connecting to database...");
        Connection conn = DBConnection.getConnection();
        
        if (conn != null) {
            System.out.println("Database Connected Successfully");
            try (Statement stmt = conn.createStatement()) {
                // List tables
                System.out.println("\n--- Tables in Database ---");
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        System.out.println("Table: " + rs.getString("TABLE_NAME"));
                    }
                }

                // List tables and their columns
                System.out.println("\n--- Tables and Columns in Database ---");
                String[] tables = {"users", "medicines", "stock", "usage_log"};
                for (String table : tables) {
                    System.out.println("\nTable: " + table);
                    try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 0")) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.println("  Column: " + metaData.getColumnName(i) + " (" + metaData.getColumnTypeName(i) + ")");
                        }
                    } catch (Exception e) {
                        System.err.println("  Failed to query table " + table + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                e.printStackTrace();
            } finally {
                DBConnection.closeConnection();
            }
        } else {
            System.out.println("Connection Failed");
        }
    }
}
