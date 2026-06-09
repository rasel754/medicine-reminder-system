package service;

import dao.MedicineDAO;
import model.Medicine;

import java.util.List;

/**
 * MedicineService encapsulates business rules for configuring and listing medications.
 */
public class MedicineService {
    private final MedicineDAO medicineDAO;

    public MedicineService() {
        this.medicineDAO = new MedicineDAO();
    }

    public boolean addMedicine(Medicine med) {
        if (med.getName() == null || med.getName().trim().isEmpty()) {
            System.err.println("Validation Error: Medicine name is empty.");
            return false;
        }
        return medicineDAO.add(med) != null;
    }

    public boolean updateMedicine(Medicine med) {
        if (med.getName() == null || med.getName().trim().isEmpty() || med.getId() <= 0) {
            System.err.println("Validation Error: Invalid medicine parameters.");
            return false;
        }
        return medicineDAO.update(med);
    }

    public boolean deleteMedicine(int id) {
        if (id <= 0) {
            return false;
        }
        return medicineDAO.delete(id);
    }

    public List<Medicine> getAllMedicines() {
        return medicineDAO.getAll();
    }

    public List<Medicine> getMedicinesForUser(int userId) {
        return medicineDAO.getByUserId(userId);
    }

    public Medicine getMedicine(int id) {
        return medicineDAO.getById(id);
    }
}
