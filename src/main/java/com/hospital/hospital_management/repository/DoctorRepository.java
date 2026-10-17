package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Doctor entity.
 * Provides standard CRUD and custom search by name, specialization, department, and contact.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Search doctors by name, specialization, department, or contact number.
     * Case-insensitive matching.
     */
    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.department) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "CAST(d.id AS string) = :keyword OR " +
           "d.contactNumber LIKE CONCAT('%', :keyword, '%')")
    List<Doctor> searchDoctors(@Param("keyword") String keyword);

    /**
     * Filter doctors by department.
     */
    List<Doctor> findByDepartmentIgnoreCase(String department);

    /**
     * Retrieve distinct department names for filtering.
     */
    @Query("SELECT DISTINCT d.department FROM Doctor d ORDER BY d.department ASC")
    List<String> findDistinctDepartments();

    /**
     * Retrieve all doctors ordered with newest registrations first.
     */
    List<Doctor> findAllByOrderByIdDesc();
}
