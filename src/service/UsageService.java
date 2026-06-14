package service;

import dao.IntakeLogDAO;
import model.UsageDTO;
import java.util.List;

/**
 * UsageService provides analytical services for medicine dosage tracking.
 */
public class UsageService {
    private final IntakeLogDAO intakeLogDAO = new IntakeLogDAO();

    /**
     * Retrieves the most used medicines list, filtered by time range (Today / Last 7 days / All time).
     */
    public List<UsageDTO> getMostUsedMedicines(String filter) {
        return intakeLogDAO.getMostUsedMedicines(filter);
    }

    /**
     * Retrieves daily taken counts.
     */
    public List<UsageDTO> getDailyUsage(String filter) {
        return intakeLogDAO.getDailyUsage(filter);
    }

    /**
     * Retrieves weekly taken counts.
     */
    public List<UsageDTO> getWeeklyUsage(String filter) {
        return intakeLogDAO.getWeeklyUsage(filter);
    }

    /**
     * Retrieves total taken doses.
     */
    public int getTakenCount(String filter) {
        return intakeLogDAO.getTakenCount(filter);
    }

    /**
     * Retrieves total logged doses (taken + missed).
     */
    public int getTotalCount(String filter) {
        return intakeLogDAO.getTotalCount(filter);
    }
}
