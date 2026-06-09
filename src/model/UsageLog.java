package model;

/**
 * UsageLog model representing records of when medication reminders were taken or skipped.
 */
public class UsageLog {
    private int id;
    private int medicineId;
    private String takenDate; // YYYY-MM-DD HH:MM:SS
    private String status;    // 'TAKEN' or 'SKIPPED'

    public UsageLog() {}

    public UsageLog(int id, int medicineId, String takenDate, String status) {
        this.id = id;
        this.medicineId = medicineId;
        this.takenDate = takenDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(String takenDate) {
        this.takenDate = takenDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
