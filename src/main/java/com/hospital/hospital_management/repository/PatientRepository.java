package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Patient entity.
 * Provides standard CRUD and custom search query.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Search patients by ID, first name, last name, or contact number.
     * Case-insensitive matching.
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(p.id AS string) = :keyword OR " +
           "p.contactNumber LIKE CONCAT('%', :keyword, '%')")
    List<Patient> searchPatients(@Param("keyword") String keyword);

    /**
     * Order patients by registration date / ID descending for newest first.
     */
    List<Patient> findAllByOrderByIdDesc();
}
