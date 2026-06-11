package model;

/**
 * IntakeLog model representing daily intake recordings of taken/missed medications.
 */
public class IntakeLog {
    private int id;
    private int userId;
    private int medicineId;
    private String status; // 'taken' or 'missed'
    private String date;   // 'YYYY-MM-DD'
    private String medicineName;
    private String medicineDosage;

    public IntakeLog() {}

    public IntakeLog(int id, int userId, int medicineId, String status, String date) {
        this.id = id;
        this.userId = userId;
        this.medicineId = medicineId;
        this.status = status;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(int medicineId) {
        this.medicineId = medicineId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getMedicineDosage() {
        return medicineDosage;
    }

    public void setMedicineDosage(String medicineDosage) {
        this.medicineDosage = medicineDosage;
    }
}
