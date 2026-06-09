package service;

import dao.MedicineDAO;
import model.Medicine;
import model.User;
import util.SoundUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ReminderService operates a background checker thread that wakes up every 60 seconds
 * to check if any medication is due at the current hour/minute.
 */
public class ReminderService {

    public interface ReminderNotificationListener {
        void onReminderTriggered(Medicine medicine, String dueTime);
    }

    private final MedicineDAO medicineDAO;
    private final List<ReminderNotificationListener> listeners = new ArrayList<>();
    private Thread workerThread;
    private boolean running = false;
    private final Set<String> triggeredKeys = new HashSet<>();

    public ReminderService() {
        this.medicineDAO = new MedicineDAO();
    }

    public synchronized void addNotificationListener(ReminderNotificationListener listener) {
        listeners.add(listener);
    }

    /**
     * Starts the periodic background checker thread. Runs every 60 seconds.
     */
    public synchronized void startChecker() {
        if (running) return;
        running = true;
        workerThread = new Thread(this::checkLoop, "ReminderService-BackgroundThread");
        workerThread.setDaemon(true);
        workerThread.start();
        System.out.println("ReminderService background checker started.");
    }

    /**
     * Stops the background thread.
     */
    public synchronized void stopChecker() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
        }
        System.out.println("ReminderService background checker stopped.");
    }

    private void checkLoop() {
        while (running) {
            try {
                checkDueMedicines();
                // Sleep for 60 seconds as required
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                System.err.println("Exception in ReminderService loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void checkDueMedicines() {
        User currentUser = AuthService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            // Get all medicines for current user
            List<Medicine> list = medicineDAO.getByUserId(currentUser.getId());
            LocalTime now = LocalTime.now();
            String currentTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            String todayStr = LocalDate.now().toString();

            for (Medicine med : list) {
                // check current time vs medicine time
                String medTime = med.getTime(); // e.g., '08:00'
                if (medTime != null && medTime.trim().equals(currentTimeStr)) {
                    String triggerKey = med.getId() + "_" + todayStr + "_" + medTime;
                    if (!triggeredKeys.contains(triggerKey)) {
                        triggeredKeys.add(triggerKey);
                        triggerNotification(med, medTime);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading medicines: " + e.getMessage());
        }
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
