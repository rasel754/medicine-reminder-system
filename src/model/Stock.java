package model;

/**
 * Stock model representing medication inventory tracking.
 */
public class Stock {
    private int id;
    private String medicineName;
    private int quantity;

    public Stock() {}

    public Stock(int id, String medicineName, int quantity) {
        this.id = id;
        this.medicineName = medicineName;
        this.quantity = quantity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
