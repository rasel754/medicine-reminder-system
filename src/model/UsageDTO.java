package model;

/**
 * UsageDTO holds aggregated analytics results (e.g. medicine usage counts, daily/weekly stats).
 */
public class UsageDTO {
    private String name;
    private int count;

    public UsageDTO() {}

    public UsageDTO(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
