package service;

import dao.MedicineDAO;
import model.Medicine;
import model.User;
import util.SoundUtil;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ReminderService operates a background checker thread that wakes up every 30 seconds
 * to check if any medication is due at the current hour/minute.
 */
public class ReminderService {

    public interface ReminderNotificationListener {
        void onReminderTriggered(Medicine medicine, String dueTime);
    }

    private final MedicineDAO medicineDAO;
    private final List<ReminderNotificationListener> listeners = new ArrayList<>();
    private ScheduledExecutorService scheduler;
    private final Set<Integer> triggeredIds = new HashSet<>();
    private String lastCheckedMinute = "";

    public ReminderService() {
        this.medicineDAO = new MedicineDAO();
    }

    public synchronized void addNotificationListener(ReminderNotificationListener listener) {
        listeners.add(listener);
    }

    /**
     * Starts the periodic background checker. Runs every 30 seconds.
     */
    public synchronized void startChecker() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        
        triggeredIds.clear();
        lastCheckedMinute = "";
        
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "ReminderService-BackgroundThread");
            t.setDaemon(true);
            return t;
        });
        
        // Setup real-time test case: time = current time + 1 minute
        setupRealTimeTestCase();
        
        scheduler.scheduleAtFixedRate(this::checkDueMedicines, 0, 30, TimeUnit.SECONDS);
        System.out.println("Scheduler started...");
    }

    /**
     * Stops the background scheduler.
     */
    public synchronized void stopChecker() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        System.out.println("ReminderService background checker stopped.");
    }

    private void setupRealTimeTestCase() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser != null && currentUser.getRole().equalsIgnoreCase("USER")) {
            try {
                LocalTime testTime = LocalTime.now().plusMinutes(1);
                String testTimeStr = testTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
                
                List<Medicine> meds = medicineDAO.getByUserId(currentUser.getId());
                boolean hasTestMed = false;
                for (Medicine m : meds) {
                    if ("Test Reminder".equals(m.getName())) {
                        m.setTime(testTimeStr);
                        medicineDAO.update(m);
                        hasTestMed = true;
                        System.out.println("[Test Setup] Updated existing 'Test Reminder' to: " + testTimeStr);
                        break;
                    }
                }
                
                if (!hasTestMed) {
                    Medicine testMed = new Medicine(0, "Test Reminder", "1 pill", testTimeStr, java.time.LocalDate.now().plusDays(30).toString(), currentUser.getId());
                    medicineDAO.add(testMed);
                    System.out.println("[Test Setup] Created new 'Test Reminder' at: " + testTimeStr + " for user ID: " + currentUser.getId());
                }
            } catch (Exception e) {
                System.err.println("Failed to create/update real-time test reminder: " + e.getMessage());
            }
        }
    }

    private void checkDueMedicines() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            LocalTime now = LocalTime.now();
            String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
            
            // Console debugging print every cycle as requested
            System.out.println("Checking time: " + currentTimeStr);

            // Handle minute change and clear same-minute duplicate tracker
            if (!currentTimeStr.equals(lastCheckedMinute)) {
                triggeredIds.clear();
                lastCheckedMinute = currentTimeStr;
            }

            // Get all medicines for current user
            List<Medicine> list = medicineDAO.getByUserId(currentUser.getId());

            for (Medicine med : list) {
                String medTime = med.getTime(); // e.g., '08:00', '10:33pm', '08:00:00'
                if (medTime != null) {
                    String normalizedMedTime = normalizeTime(medTime);
                    if (normalizedMedTime != null && normalizedMedTime.equals(currentTimeStr)) {
                        if (!triggeredIds.contains(med.getId())) {
                            triggeredIds.add(med.getId());
                            // Print "MATCH FOUND!" as requested in debugging checklist
                            System.out.println("MATCH FOUND! Reminder triggered for " + med.getName());
                            triggerNotification(med, medTime);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading medicines: " + e.getMessage());
        }
    }

    private String normalizeTime(String timeStr) {
        if (timeStr == null) return null;
        timeStr = timeStr.trim().toLowerCase();
        
        // Try parsing HH:mm:ss
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}
        
        // Try parsing HH:mm
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}

        // Try parsing h:mm (e.g. 9:30)
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}
        
        // Try parsing hh:mma (e.g. 10:33pm)
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("hh:mma", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}
        
        // Try parsing h:mma (e.g. 8:30am)
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}
        
        // Try parsing hh:mm a (e.g. 10:33 pm)
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}

        // Try parsing h:mm a (e.g. 8:30 am)
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH));
        } catch (Exception ignored) {}
        
        return timeStr;
    }

    private void triggerNotification(Medicine medicine, String dueTime) {
        // 1. Play alert sound using SoundUtil
        SoundUtil.playAlert();

        // 2. Trigger notification method callbacks
        synchronized (listeners) {
            for (ReminderNotificationListener listener : listeners) {
                new Thread(() -> listener.onReminderTriggered(medicine, dueTime)).start();
            }
        }
    }
}
