package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentReference(String paymentReference);

    Optional<Payment> findByPaymentReference(String paymentReference);

    boolean existsByTransactionReference(String transactionReference);

    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    List<Payment> findAllByOrderByPaymentDateDesc();

    /**
     * Report query with date/time range and payment method filtering.
     */
    @Query("SELECT p FROM Payment p WHERE " +
           "(:startDateTime IS NULL OR p.paymentDate >= :startDateTime) AND " +
           "(:endDateTime IS NULL OR p.paymentDate <= :endDateTime) AND " +
           "(:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) " +
           "ORDER BY p.paymentDate DESC")
    List<Payment> findPaymentsForReport(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("paymentMethod") PaymentMethod paymentMethod);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE " +
           "(:startDateTime IS NULL OR p.paymentDate >= :startDateTime) AND " +
           "(:endDateTime IS NULL OR p.paymentDate <= :endDateTime)")
    BigDecimal sumAmountBetween(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);
}
