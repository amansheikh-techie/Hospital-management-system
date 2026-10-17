package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.BillStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    boolean existsByBillNumber(String billNumber);

    boolean existsByAppointmentId(Long appointmentId);

    boolean existsByAdmissionId(Long admissionId);

    Optional<Bill> findByBillNumber(String billNumber);

    Optional<Bill> findByAppointmentId(Long appointmentId);

    Optional<Bill> findByAdmissionId(Long admissionId);

    List<Bill> findAllByOrderByBillDateDesc();

    List<Bill> findByStatusOrderByBillDateDesc(BillStatus status);

    List<Bill> findByPatientIdOrderByBillDateDesc(Long patientId);

    @Query("SELECT b FROM Bill b WHERE " +
           "(:query IS NULL OR LOWER(b.billNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.patient.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.patient.lastName) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR b.status = :status) " +
           "ORDER BY b.billDate DESC")
    List<Bill> searchAndFilterBills(@Param("query") String query,
                                   @Param("status") BillStatus status);

    /**
     * Pessimistic write lock query to acquire an exclusive row lock on the bill
     * during payment processing, ensuring consistent balance calculation and preventing race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.id = :id")
    Optional<Bill> findByIdWithLock(@Param("id") Long id);

    long countByStatus(BillStatus status);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b")
    BigDecimal sumTotalBilledAmount();

    @Query("SELECT COALESCE(SUM(b.paidAmount), 0) FROM Bill b")
    BigDecimal sumTotalCollectedAmount();

    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM Bill b")
    BigDecimal sumTotalOutstandingBalance();
}
