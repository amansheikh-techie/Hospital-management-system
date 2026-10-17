package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedStatus;
import com.hospital.hospital_management.model.BedType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BedRepository extends JpaRepository<Bed, Long> {

    boolean existsByBedNumber(String bedNumber);

    boolean existsByBedNumberAndIdNot(String bedNumber, Long id);

    Optional<Bed> findByBedNumber(String bedNumber);

    List<Bed> findByStatusOrderByBedNumberAsc(BedStatus status);

    @Query("SELECT DISTINCT b.ward FROM Bed b ORDER BY b.ward ASC")
    List<String> findDistinctWards();

    @Query("SELECT b FROM Bed b WHERE " +
           "(:query IS NULL OR LOWER(b.bedNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.ward) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:ward IS NULL OR b.ward = :ward) AND " +
           "(:type IS NULL OR b.bedType = :type) AND " +
           "(:status IS NULL OR b.status = :status) " +
           "ORDER BY b.ward ASC, b.bedNumber ASC")
    List<Bed> searchAndFilterBeds(@Param("query") String query,
                                 @Param("ward") String ward,
                                 @Param("type") BedType type,
                                 @Param("status") BedStatus status);

    long countByStatus(BedStatus status);

    /**
     * Pessimistic write lock query to acquire an exclusive database row lock
     * during patient admission, preventing race conditions on bed allocation.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b WHERE b.id = :id")
    Optional<Bed> findByIdWithLock(@Param("id") Long id);
}
