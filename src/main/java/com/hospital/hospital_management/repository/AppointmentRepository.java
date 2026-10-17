package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Spring Data JPA Repository for Appointment entity.
 * Provides custom conflict detection and search/filter queries.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Finds overlapping appointments for a doctor on a specific date.
     * Excludes cancelled appointments and optionally excludes a specific appointment ID (for edits).
     *
     * Interval overlap rule: [startA, endA) overlaps [startB, endB) <=> startA < endB AND endA > startB
     */
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId " +
           "AND a.appointmentDate = :appointmentDate " +
           "AND a.status <> com.hospital.hospital_management.model.AppointmentStatus.CANCELLED " +
           "AND a.startTime < :endTime AND a.endTime > :startTime " +
           "AND (:excludeId IS NULL OR a.id <> :excludeId)")
    List<Appointment> findOverlappingAppointments(
            @Param("doctorId") Long doctorId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);

    /**
     * Retrieve all appointments ordered with newest date and start time first.
     */
    List<Appointment> findAllByOrderByAppointmentDateDescStartTimeDesc();

    /**
     * Search and filter appointments by optional criteria.
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:patientId IS NULL OR a.patient.id = :patientId) AND " +
           "(:appointmentDate IS NULL OR a.appointmentDate = :appointmentDate) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.doctor.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.doctor.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(a.reason) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.appointmentDate DESC, a.startTime DESC")
    List<Appointment> filterAppointments(
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("appointmentDate") LocalDate appointmentDate,
            @Param("status") AppointmentStatus status,
            @Param("keyword") String keyword);

    /**
     * Count appointments on a specific date (used for today's count on the dashboard).
     */
    long countByAppointmentDate(LocalDate appointmentDate);

    /**
     * Count appointments by status.
     */
    long countByStatus(AppointmentStatus status);

    /**
     * Report query with date range, optional doctor, and optional status filtering.
     */
    @Query("SELECT a FROM Appointment a WHERE " +
           "(:startDate IS NULL OR a.appointmentDate >= :startDate) AND " +
           "(:endDate IS NULL OR a.appointmentDate <= :endDate) AND " +
           "(:doctorId IS NULL OR a.doctor.id = :doctorId) AND " +
           "(:status IS NULL OR a.status = :status) " +
           "ORDER BY a.appointmentDate DESC, a.startTime DESC")
    List<Appointment> findAppointmentsForReport(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("doctorId") Long doctorId,
            @Param("status") AppointmentStatus status);
}
