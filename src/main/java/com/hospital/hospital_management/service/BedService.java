package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Admission;
import com.hospital.hospital_management.model.AdmissionStatus;
import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedStatus;
import com.hospital.hospital_management.model.BedType;
import com.hospital.hospital_management.repository.AdmissionRepository;
import com.hospital.hospital_management.repository.BedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing hospital beds and their operational lifecycle.
 */
@Service
@Transactional
public class BedService {

    private final BedRepository bedRepository;
    private final AdmissionRepository admissionRepository;

    @Autowired
    public BedService(BedRepository bedRepository, AdmissionRepository admissionRepository) {
        this.bedRepository = bedRepository;
        this.admissionRepository = admissionRepository;
    }

    /**
     * Registers a new bed after verifying bed number uniqueness.
     */
    public Bed createBed(Bed bed) {
        if (bed.getBedNumber() == null || bed.getBedNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Bed number is required");
        }
        String cleanNumber = bed.getBedNumber().trim();
        if (bedRepository.existsByBedNumber(cleanNumber)) {
            throw new IllegalArgumentException("Bed with number '" + cleanNumber + "' already exists");
        }
        bed.setBedNumber(cleanNumber);
        if (bed.getStatus() == null) {
            bed.setStatus(BedStatus.AVAILABLE);
        }
        return bedRepository.save(bed);
    }

    /**
     * Updates existing bed details while protecting active admission constraints.
     */
    public Bed updateBed(Long id, Bed updatedBed) {
        Bed existingBed = bedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found with ID: " + id));

        String cleanNumber = updatedBed.getBedNumber() != null ? updatedBed.getBedNumber().trim() : "";
        if (cleanNumber.isEmpty()) {
            throw new IllegalArgumentException("Bed number is required");
        }
        if (bedRepository.existsByBedNumberAndIdNot(cleanNumber, id)) {
            throw new IllegalArgumentException("Bed with number '" + cleanNumber + "' already exists");
        }

        // Rule: Prevent manually marking AVAILABLE or MAINTENANCE while active admission uses it
        boolean hasActiveAdmission = admissionRepository.existsByBedIdAndStatus(id, AdmissionStatus.ACTIVE);
        if (hasActiveAdmission && !BedStatus.OCCUPIED.equals(updatedBed.getStatus())) {
            throw new IllegalStateException("Cannot change bed status to '" + updatedBed.getStatus() +
                    "' while an active patient admission is assigned to this bed");
        }

        existingBed.setBedNumber(cleanNumber);
        existingBed.setWard(updatedBed.getWard());
        existingBed.setBedType(updatedBed.getBedType());
        existingBed.setFloor(updatedBed.getFloor());
        existingBed.setDailyCharge(updatedBed.getDailyCharge());
        existingBed.setDescription(updatedBed.getDescription());

        // Only update status if not locked by active admission
        if (!hasActiveAdmission) {
            existingBed.setStatus(updatedBed.getStatus());
        }

        return bedRepository.save(existingBed);
    }

    /**
     * Marks an unoccupied bed as under maintenance.
     */
    public Bed markUnderMaintenance(Long id) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found with ID: " + id));

        if (admissionRepository.existsByBedIdAndStatus(id, AdmissionStatus.ACTIVE) || BedStatus.OCCUPIED.equals(bed.getStatus())) {
            throw new IllegalStateException("Cannot mark bed '" + bed.getBedNumber() +
                    "' under maintenance because it is currently occupied by an admitted patient");
        }

        bed.setStatus(BedStatus.MAINTENANCE);
        return bedRepository.save(bed);
    }

    /**
     * Returns a maintenance bed back to AVAILABLE.
     */
    public Bed returnToAvailable(Long id) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found with ID: " + id));

        if (admissionRepository.existsByBedIdAndStatus(id, AdmissionStatus.ACTIVE)) {
            throw new IllegalStateException("Cannot mark bed '" + bed.getBedNumber() +
                    "' available because an active admission exists");
        }

        bed.setStatus(BedStatus.AVAILABLE);
        return bedRepository.save(bed);
    }

    /**
     * Deletes a bed if safe (no active or historical admissions exist).
     */
    public void deleteBed(Long id) {
        Bed bed = bedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found with ID: " + id));

        if (admissionRepository.existsByBedId(id)) {
            throw new IllegalStateException("Cannot delete bed '" + bed.getBedNumber() +
                    "' because it has historical or active admission records. Set it to MAINTENANCE instead.");
        }

        bedRepository.delete(bed);
    }

    @Transactional(readOnly = true)
    public List<Bed> getAllBeds() {
        return bedRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Bed> getBedById(Long id) {
        return bedRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Bed> getAvailableBeds() {
        return bedRepository.findByStatusOrderByBedNumberAsc(BedStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctWards() {
        return bedRepository.findDistinctWards();
    }

    @Transactional(readOnly = true)
    public List<Bed> searchAndFilterBeds(String query, String ward, BedType type, BedStatus status) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        String cleanWard = (ward != null && !ward.trim().isEmpty()) ? ward.trim() : null;
        return bedRepository.searchAndFilterBeds(cleanQuery, cleanWard, type, status);
    }

    @Transactional(readOnly = true)
    public Optional<Admission> getActiveAdmissionForBed(Long bedId) {
        return admissionRepository.findByBedIdAndStatus(bedId, AdmissionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<Admission> getAdmissionHistoryForBed(Long bedId) {
        return admissionRepository.findByBedIdOrderByAdmissionDateDesc(bedId);
    }

    // Dashboard metrics
    @Transactional(readOnly = true)
    public long getTotalBedCount() {
        return bedRepository.count();
    }

    @Transactional(readOnly = true)
    public long getAvailableBedCount() {
        return bedRepository.countByStatus(BedStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public long getOccupiedBedCount() {
        return bedRepository.countByStatus(BedStatus.OCCUPIED);
    }

    @Transactional(readOnly = true)
    public long getMaintenanceBedCount() {
        return bedRepository.countByStatus(BedStatus.MAINTENANCE);
    }
}
