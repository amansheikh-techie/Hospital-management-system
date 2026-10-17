package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;
import com.hospital.hospital_management.repository.AppointmentRepository;
import com.hospital.hospital_management.repository.DoctorRepository;
import com.hospital.hospital_management.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for Appointment management and Schedule Conflict Prevention.
 *
 * Implements strict interval overlap checks to prevent double-booking doctors.
 */
@Service
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Retrieve all appointments ordered by date and start time descending.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateDescStartTimeDesc();
    }

    /**
     * Filter appointments by doctor, patient, date, status, or keyword.
     */
    @Transactional(readOnly = true)
    public List<Appointment> filterAppointments(Long doctorId, Long patientId,
                                                LocalDate date, AppointmentStatus status,
                                                String keyword) {
        return appointmentRepository.filterAppointments(doctorId, patientId, date, status, keyword);
    }

    /**
     * Retrieve appointment by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Book a new appointment with schedule conflict prevention.
     */
    public Appointment bookAppointment(Appointment appointment) {
        validateAppointmentEntities(appointment);
        validateTimeInterval(appointment.getStartTime(), appointment.getEndTime());
        checkForScheduleConflicts(appointment.getDoctor().getId(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                null);

        return appointmentRepository.save(appointment);
    }

    /**
     * Update/reschedule an existing appointment with conflict checks (excluding itself).
     */
    public Appointment updateAppointment(Long id, Appointment updatedData) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + id));

        validateAppointmentEntities(updatedData);
        validateTimeInterval(updatedData.getStartTime(), updatedData.getEndTime());
        checkForScheduleConflicts(updatedData.getDoctor().getId(),
                updatedData.getAppointmentDate(),
                updatedData.getStartTime(),
                updatedData.getEndTime(),
                id);

        existing.setPatient(updatedData.getPatient());
        existing.setDoctor(updatedData.getDoctor());
        existing.setAppointmentDate(updatedData.getAppointmentDate());
        existing.setStartTime(updatedData.getStartTime());
        existing.setEndTime(updatedData.getEndTime());
        existing.setReason(updatedData.getReason());
        if (updatedData.getStatus() != null) {
            existing.setStatus(updatedData.getStatus());
        }

        return appointmentRepository.save(existing);
    }

    /**
     * Safely cancel an appointment. Cancelled appointments no longer block doctor schedules.
     */
    public Appointment cancelAppointment(Long id) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + id));
        existing.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(existing);
    }

    /**
     * Mark an appointment as completed.
     */
    public Appointment completeAppointment(Long id) {
        Appointment existing = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + id));
        existing.setStatus(AppointmentStatus.COMPLETED);
        return appointmentRepository.save(existing);
    }

    /**
     * Safely delete an appointment record.
     */
    public boolean deleteAppointment(Long id) {
        if (appointmentRepository.existsById(id)) {
            appointmentRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Count of appointments scheduled for today (for dashboard metric).
     */
    @Transactional(readOnly = true)
    public long getTodayAppointmentCount() {
        return appointmentRepository.countByAppointmentDate(LocalDate.now());
    }

    /**
     * Total count of all appointments across the system.
     */
    @Transactional(readOnly = true)
    public long getTotalAppointmentCount() {
        return appointmentRepository.count();
    }

    // ------------------------------------------------------------------------
    // Validation & Conflict Detection Helpers
    // ------------------------------------------------------------------------

    private void validateAppointmentEntities(Appointment appointment) {
        if (appointment.getPatient() == null || appointment.getPatient().getId() == null) {
            throw new IllegalArgumentException("A valid patient must be selected.");
        }
        if (!patientRepository.existsById(appointment.getPatient().getId())) {
            throw new IllegalArgumentException("Selected patient does not exist (ID: " + appointment.getPatient().getId() + ").");
        }

        if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null) {
            throw new IllegalArgumentException("A valid doctor must be selected.");
        }
        if (!doctorRepository.existsById(appointment.getDoctor().getId())) {
            throw new IllegalArgumentException("Selected doctor does not exist (ID: " + appointment.getDoctor().getId() + ").");
        }
    }

    public void validateTimeInterval(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Both start time and end time are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time (" + endTime + ") must be after start time (" + startTime + ").");
        }
    }

    public void checkForScheduleConflicts(Long doctorId, LocalDate date, LocalTime startTime, LocalTime endTime, Long excludeId) {
        List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                doctorId, date, startTime, endTime, excludeId);

        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            throw new IllegalStateException(
                    "Doctor schedule conflict: " + conflict.getDoctor().getDisplayName() +
                    " already has an active appointment on " + date + " from " +
                    conflict.getStartTime() + " to " + conflict.getEndTime() +
                    " (" + conflict.getStatus().getDisplayName() + "). " +
                    "Please choose another time slot or doctor."
            );
        }
    }
}
