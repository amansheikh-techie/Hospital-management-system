package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Admission;
import com.hospital.hospital_management.model.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    boolean existsByBedIdAndStatus(Long bedId, AdmissionStatus status);

    boolean existsByPatientIdAndStatus(Long patientId, AdmissionStatus status);

    boolean existsByBedId(Long bedId);

    Optional<Admission> findByBedIdAndStatus(Long bedId, AdmissionStatus status);

    Optional<Admission> findByPatientIdAndStatus(Long patientId, AdmissionStatus status);

    List<Admission> findByBedIdOrderByAdmissionDateDesc(Long bedId);

    List<Admission> findByPatientIdOrderByAdmissionDateDesc(Long patientId);

    List<Admission> findAllByOrderByAdmissionDateDesc();

    List<Admission> findByStatusOrderByAdmissionDateDesc(AdmissionStatus status);

    @Query("SELECT a FROM Admission a WHERE " +
           "(:patientName IS NULL OR LOWER(a.patient.firstName) LIKE LOWER(CONCAT('%', :patientName, '%')) OR LOWER(a.patient.lastName) LIKE LOWER(CONCAT('%', :patientName, '%'))) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:ward IS NULL OR a.bed.ward = :ward) " +
           "ORDER BY a.admissionDate DESC")
    List<Admission> searchAndFilterAdmissions(@Param("patientName") String patientName,
                                             @Param("status") AdmissionStatus status,
                                             @Param("ward") String ward);

    long countByStatus(AdmissionStatus status);

    long countByAdmissionDateBetween(LocalDateTime start, LocalDateTime end);
}
