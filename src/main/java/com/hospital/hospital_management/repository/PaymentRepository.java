package com.hospital.hospital_management.repository;

import com.hospital.hospital_management.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentReference(String paymentReference);

    Optional<Payment> findByPaymentReference(String paymentReference);

    boolean existsByTransactionReference(String transactionReference);

    List<Payment> findByBillIdOrderByPaymentDateDesc(Long billId);

    List<Payment> findAllByOrderByPaymentDateDesc();
}
