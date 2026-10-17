package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling Patient business logic and transactions.
 */
@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    @Autowired
    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Get all patients, ordered with newest registrations first.
     */
    @Transactional(readOnly = true)
    public List<Patient> getAllPatients() {
        return patientRepository.findAllByOrderByIdDesc();
    }

    /**
     * Search patients by keyword (ID, name, or contact number).
     */
    @Transactional(readOnly = true)
    public List<Patient> searchPatients(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllPatients();
        }
        return patientRepository.searchPatients(keyword.trim());
    }

    /**
     * Retrieve a patient by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    /**
     * Register a new patient.
     */
    public Patient savePatient(Patient patient) {
        return patientRepository.save(patient);
    }

    /**
     * Update an existing patient's details.
     */
    public Patient updatePatient(Long id, Patient updatedData) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + id));

        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        existing.setAge(updatedData.getAge());
        existing.setGender(updatedData.getGender());
        existing.setBloodGroup(updatedData.getBloodGroup());
        existing.setContactNumber(updatedData.getContactNumber());
        existing.setEmail(updatedData.getEmail());
        existing.setAddress(updatedData.getAddress());
        existing.setMedicalHistory(updatedData.getMedicalHistory());

        return patientRepository.save(existing);
    }

    /**
     * Safely delete a patient.
     * Returns true if deleted, false if patient did not exist.
     */
    public boolean deletePatient(Long id) {
        if (patientRepository.existsById(id)) {
            patientRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Get total count of registered patients for the dashboard.
     */
    @Transactional(readOnly = true)
    public long getTotalPatientCount() {
        return patientRepository.count();
    }
}
