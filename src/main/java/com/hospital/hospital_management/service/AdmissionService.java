package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.repository.AdmissionRepository;
import com.hospital.hospital_management.repository.BedRepository;
import com.hospital.hospital_management.repository.DoctorRepository;
import com.hospital.hospital_management.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing inpatient patient admissions and discharges.
 *
 * Implements strict transaction boundaries and pessimistic locking on bed allocation
 * to prevent concurrent over-allocation of hospital beds.
 */
@Service
@Transactional
public class AdmissionService {

    private final AdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public AdmissionService(AdmissionRepository admissionRepository,
                            BedRepository bedRepository,
                            PatientRepository patientRepository,
                            DoctorRepository doctorRepository) {
        this.admissionRepository = admissionRepository;
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Admits a patient to a bed in an atomic transaction.
     * Uses pessimistic locking to prevent race conditions during concurrent bed bookings.
     */
    public Admission admitPatient(Long patientId, Long bedId, Long doctorId,
                                  LocalDateTime admissionDate, String admissionReason) {
        // 1. Validate Patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + patientId));

        // 2. Rule: Prevent multiple active admissions for the same patient
        Optional<Admission> activePatientAdmission = admissionRepository.findByPatientIdAndStatus(patientId, AdmissionStatus.ACTIVE);
        if (activePatientAdmission.isPresent()) {
            Admission current = activePatientAdmission.get();
            throw new IllegalStateException("Patient " + patient.getFirstName() + " " + patient.getLastName() +
                    " (ID: #" + patient.getId() + ") already has an active admission (Admission ID: #" + current.getId() +
                    ") in Bed " + current.getBed().getBedNumber() + " (" + current.getBed().getWard() + ")");
        }

        // 3. Acquire pessimistic lock on the Bed to guarantee isolation
        Bed bed = bedRepository.findByIdWithLock(bedId)
                .orElseThrow(() -> new IllegalArgumentException("Bed not found with ID: " + bedId));

        // 4. Validate Bed status
        if (!BedStatus.AVAILABLE.equals(bed.getStatus())) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is not available for admission (current status: " +
                    bed.getStatus().getDisplayName() + ")");
        }

        // 5. Double-check no active admission exists for this bed
        if (admissionRepository.existsByBedIdAndStatus(bedId, AdmissionStatus.ACTIVE)) {
            throw new IllegalStateException("Bed " + bed.getBedNumber() + " is already occupied by an active admission");
        }

        // 6. Optional attending doctor
        Doctor doctor = null;
        if (doctorId != null) {
            doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Attending doctor not found with ID: " + doctorId));
        }

        // 7. Create admission record
        Admission admission = new Admission();
        admission.setPatient(patient);
        admission.setBed(bed);
        admission.setAttendingDoctor(doctor);
        admission.setAdmissionDate(admissionDate != null ? admissionDate : LocalDateTime.now());
        admission.setAdmissionReason(admissionReason);
        admission.setStatus(AdmissionStatus.ACTIVE);

        // 8. Update bed status to OCCUPIED
        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        return admissionRepository.save(admission);
    }

    /**
     * Discharges an active inpatient stay and releases the bed.
     */
    public Admission dischargePatient(Long admissionId, LocalDateTime dischargeDate, String dischargeNotes) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found with ID: " + admissionId));

        if (!AdmissionStatus.ACTIVE.equals(admission.getStatus())) {
            throw new IllegalStateException("Cannot discharge admission #" + admissionId +
                    " because it is already marked as " + admission.getStatus().getDisplayName());
        }

        LocalDateTime actualDischargeDate = dischargeDate != null ? dischargeDate : LocalDateTime.now();
        if (actualDischargeDate.isBefore(admission.getAdmissionDate())) {
            throw new IllegalArgumentException("Discharge date/time (" + actualDischargeDate +
                    ") cannot be earlier than admission date/time (" + admission.getAdmissionDate() + ")");
        }

        admission.setDischargeDate(actualDischargeDate);
        admission.setDischargeNotes(dischargeNotes);
        admission.setStatus(AdmissionStatus.DISCHARGED);

        // Release the assigned bed to AVAILABLE (unless it was explicitly flagged MAINTENANCE)
        Bed bed = admission.getBed();
        if (bed != null && BedStatus.OCCUPIED.equals(bed.getStatus())) {
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
        }

        return admissionRepository.save(admission);
    }

    @Transactional(readOnly = true)
    public List<Admission> getAllAdmissions() {
        return admissionRepository.findAllByOrderByAdmissionDateDesc();
    }

    @Transactional(readOnly = true)
    public List<Admission> getActiveAdmissions() {
        return admissionRepository.findByStatusOrderByAdmissionDateDesc(AdmissionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Admission> getAdmissionById(Long id) {
        return admissionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Admission> searchAndFilterAdmissions(String patientName, AdmissionStatus status, String ward) {
        String cleanName = (patientName != null && !patientName.trim().isEmpty()) ? patientName.trim() : null;
        String cleanWard = (ward != null && !ward.trim().isEmpty()) ? ward.trim() : null;
        return admissionRepository.searchAndFilterAdmissions(cleanName, status, cleanWard);
    }

    @Transactional(readOnly = true)
    public long getActiveAdmissionCount() {
        return admissionRepository.countByStatus(AdmissionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long getTodayAdmissionCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        return admissionRepository.countByAdmissionDateBetween(startOfDay, endOfDay);
    }
}
