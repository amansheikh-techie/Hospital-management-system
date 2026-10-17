package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Doctor management business logic.
 */
@Service
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Autowired
    public DoctorService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    /**
     * Retrieve all registered doctors.
     */
    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAllByOrderByIdDesc();
    }

    /**
     * Search doctors by name, specialization, department, or phone.
     */
    @Transactional(readOnly = true)
    public List<Doctor> searchDoctors(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllDoctors();
        }
        return doctorRepository.searchDoctors(keyword.trim());
    }

    /**
     * Retrieve a doctor by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    /**
     * Register a new doctor.
     */
    public Doctor saveDoctor(Doctor doctor) {
        return doctorRepository.save(doctor);
    }

    /**
     * Update an existing doctor's details.
     */
    public Doctor updateDoctor(Long id, Doctor updatedData) {
        Doctor existing = doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with ID: " + id));

        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        existing.setSpecialization(updatedData.getSpecialization());
        existing.setDepartment(updatedData.getDepartment());
        existing.setContactNumber(updatedData.getContactNumber());
        existing.setEmail(updatedData.getEmail());
        existing.setConsultationFee(updatedData.getConsultationFee());
        existing.setAvailableDays(updatedData.getAvailableDays());

        return doctorRepository.save(existing);
    }

    /**
     * Safely delete a doctor.
     */
    public boolean deleteDoctor(Long id) {
        if (doctorRepository.existsById(id)) {
            doctorRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Total count of doctors for dashboard metrics.
     */
    @Transactional(readOnly = true)
    public long getTotalDoctorCount() {
        return doctorRepository.count();
    }

    /**
     * Retrieve distinct departments.
     */
    @Transactional(readOnly = true)
    public List<String> getAllDepartments() {
        return doctorRepository.findDistinctDepartments();
    }
}
