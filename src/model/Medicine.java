package model;

/**
 * Medicine model representing active medication reminders.
 */
public class Medicine {
    private int id;
    private String name;
    private String dosage;
    private String time;        // Time of day (e.g., "08:00")
    private String expiryDate;  // Expiry date (e.g., "2026-12-31")
    private int userId;

    public Medicine() {}

    public Medicine(int id, String name, String dosage, String time, String expiryDate, int userId) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.time = time;
        this.expiryDate = expiryDate;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
