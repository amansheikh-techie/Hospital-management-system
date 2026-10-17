package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing invoices, line items, patient billing, and payment processing.
 *
 * Implements strict transaction boundaries and pessimistic locking for payment concurrency.
 */
@Service
@Transactional
public class BillingService {

    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdmissionRepository admissionRepository;

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    public BillingService(BillRepository billRepository,
                          PaymentRepository paymentRepository,
                          PatientRepository patientRepository,
                          AppointmentRepository appointmentRepository,
                          AdmissionRepository admissionRepository) {
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.admissionRepository = admissionRepository;
    }

    /**
     * Generates a unique invoice number: INV-YYYYMMDD-XXXX
     */
    public synchronized String generateBillNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = SEQUENCE.incrementAndGet() % 10000;
        String num = String.format("INV-%s-%04d", dateStr, seq);
        while (billRepository.existsByBillNumber(num)) {
            seq = SEQUENCE.incrementAndGet() % 10000;
            num = String.format("INV-%s-%04d", dateStr, seq);
        }
        return num;
    }

    /**
     * Generates a unique payment reference: PAY-YYYYMMDD-XXXX
     */
    public synchronized String generatePaymentReference() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = SEQUENCE.incrementAndGet() % 10000;
        String ref = String.format("PAY-%s-%04d", dateStr, seq);
        while (paymentRepository.existsByPaymentReference(ref)) {
            seq = SEQUENCE.incrementAndGet() % 10000;
            ref = String.format("PAY-%s-%04d", dateStr, seq);
        }
        return ref;
    }

    /**
     * Creates a standard or manual bill with custom line items.
     */
    public Bill createBill(Long patientId, List<BillItem> items,
                           BigDecimal discountAmount, BigDecimal taxAmount, String notes) {
        return createManualBill(patientId, items, discountAmount, taxAmount, notes);
    }

    public Bill createManualBill(Long patientId, List<BillItem> items,
                                 BigDecimal discountAmount, BigDecimal taxAmount, String notes) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + patientId));

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An invoice must contain at least one line item.");
        }

        Bill bill = new Bill();
        bill.setBillNumber(generateBillNumber());
        bill.setPatient(patient);
        bill.setBillDate(LocalDateTime.now());
        bill.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        bill.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        bill.setNotes(notes);

        for (BillItem item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new IllegalArgumentException("Item quantity must be at least 1");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Unit price cannot be negative");
            }
            bill.addItem(item);
        }

        bill.calculateTotals();
        return billRepository.save(bill);
    }

    /**
     * Generates an invoice snapshot from an existing Doctor Appointment.
     * Prevents duplicate billing for the same appointment.
     */
    public Bill createBillForAppointment(Long appointmentId, BigDecimal discountAmount,
                                         BigDecimal taxAmount, String notes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        if (billRepository.existsByAppointmentId(appointmentId)) {
            Bill existing = billRepository.findByAppointmentId(appointmentId).orElse(null);
            String inv = existing != null ? existing.getBillNumber() : "existing";
            throw new IllegalStateException("An invoice has already been generated for Appointment #" +
                    appointmentId + " (Invoice: " + inv + ").");
        }

        Doctor doctor = appointment.getDoctor();
        BigDecimal consultationFee = doctor.getConsultationFee() != null ?
                BigDecimal.valueOf(doctor.getConsultationFee()).setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill();
        bill.setBillNumber(generateBillNumber());
        bill.setPatient(appointment.getPatient());
        bill.setAppointment(appointment);
        bill.setBillDate(LocalDateTime.now());
        bill.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        bill.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        bill.setNotes(notes != null && !notes.trim().isEmpty() ? notes.trim() :
                "Consultation fee for Appointment #" + appointmentId + " on " +
                        appointment.getAppointmentDate() + " with Dr. " + doctor.getFirstName() + " " + doctor.getLastName());

        // Snapshot line item
        String desc = "Doctor Consultation - Dr. " + doctor.getFirstName() + " " + doctor.getLastName() +
                " (" + doctor.getSpecialization() + ")";
        BillItem item = new BillItem(desc, 1, consultationFee);
        bill.addItem(item);

        bill.calculateTotals();
        return billRepository.save(bill);
    }

    /**
     * Generates an invoice snapshot from an inpatient Admission.
     * Calculates stay duration according to defined hospital day-counting rules.
     * Prevents duplicate billing for the same admission.
     */
    public Bill createBillForAdmission(Long admissionId, BigDecimal discountAmount,
                                       BigDecimal taxAmount, String notes) {
        Admission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new IllegalArgumentException("Admission not found with ID: " + admissionId));

        if (billRepository.existsByAdmissionId(admissionId)) {
            Bill existing = billRepository.findByAdmissionId(admissionId).orElse(null);
            String inv = existing != null ? existing.getBillNumber() : "existing";
            throw new IllegalStateException("An invoice has already been generated for Admission #" +
                    admissionId + " (Invoice: " + inv + ").");
        }

        // Calculate stay duration
        LocalDate startDate = admission.getAdmissionDate().toLocalDate();
        LocalDate endDate = (admission.getDischargeDate() != null) ?
                admission.getDischargeDate().toLocalDate() : LocalDate.now();
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0) {
            days = 1; // Same-day stay / observation charged a minimum of 1 day
        }

        Bed bed = admission.getBed();
        BigDecimal dailyCharge = bed.getDailyCharge() != null ?
                bed.getDailyCharge().setScale(2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        Bill bill = new Bill();
        bill.setBillNumber(generateBillNumber());
        bill.setPatient(admission.getPatient());
        bill.setAdmission(admission);
        bill.setBillDate(LocalDateTime.now());
        bill.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        bill.setTaxAmount(taxAmount != null ? taxAmount : BigDecimal.ZERO);

        boolean isOngoing = !AdmissionStatus.DISCHARGED.equals(admission.getStatus());
        bill.setPreliminary(isOngoing);

        String autoNotes = isOngoing ?
                "Preliminary / Interim Inpatient Invoice for Admission #" + admissionId + " (Ongoing stay as of " + LocalDate.now() + ")." :
                "Final Inpatient Discharge Invoice for Admission #" + admissionId + ".";
        bill.setNotes(notes != null && !notes.trim().isEmpty() ? notes.trim() : autoNotes);

        // Item 1: Bed stay charge
        String bedDesc = "Bed & Room Charges - Bed " + bed.getBedNumber() + " (" + bed.getWard() + " - " +
                bed.getBedType().getDisplayName() + ", " + days + " " + (days == 1 ? "day" : "days") + ")";
        BillItem bedItem = new BillItem(bedDesc, (int) days, dailyCharge);
        bill.addItem(bedItem);

        // Item 2: Attending Doctor Consultation (if assigned)
        if (admission.getAttendingDoctor() != null) {
            Doctor doc = admission.getAttendingDoctor();
            BigDecimal docFee = doc.getConsultationFee() != null ?
                    BigDecimal.valueOf(doc.getConsultationFee()).setScale(2, RoundingMode.HALF_UP) :
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            String docDesc = "Attending Physician Care - Dr. " + doc.getFirstName() + " " + doc.getLastName() +
                    " (" + doc.getSpecialization() + ")";
            BillItem docItem = new BillItem(docDesc, 1, docFee);
            bill.addItem(docItem);
        }

        bill.calculateTotals();
        return billRepository.save(bill);
    }

    /**
     * Records a payment against a bill using pessimistic row locking to prevent overpayments.
     */
    public Payment recordPayment(Long billId, BigDecimal amount, PaymentMethod paymentMethod,
                                 String transactionReference, String notes) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        // Acquire exclusive pessimistic write lock on the bill
        Bill bill = billRepository.findByIdWithLock(billId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + billId));

        BigDecimal cleanAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (cleanAmount.compareTo(bill.getBalanceAmount()) > 0) {
            throw new IllegalArgumentException("Payment amount (₹" + cleanAmount +
                    ") exceeds outstanding balance (₹" + bill.getBalanceAmount() + ").");
        }

        String cleanTxnRef = (transactionReference != null && !transactionReference.trim().isEmpty()) ?
                transactionReference.trim() : null;
        if (cleanTxnRef != null && paymentRepository.existsByTransactionReference(cleanTxnRef)) {
            throw new IllegalArgumentException("A transaction with reference '" + cleanTxnRef +
                    "' has already been recorded.");
        }

        Payment payment = new Payment();
        payment.setPaymentReference(generatePaymentReference());
        payment.setBill(bill);
        payment.setAmount(cleanAmount);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : PaymentMethod.CASH);
        payment.setTransactionReference(cleanTxnRef);
        payment.setNotes(notes != null ? notes.trim() : null);

        // Update bill figures
        BigDecimal newPaid = bill.getPaidAmount().add(cleanAmount).setScale(2, RoundingMode.HALF_UP);
        bill.setPaidAmount(newPaid);
        bill.setBalanceAmount(bill.getTotalAmount().subtract(newPaid).setScale(2, RoundingMode.HALF_UP));
        bill.updateStatusFromBalance();

        billRepository.save(bill);
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<Bill> getAllBills() {
        return billRepository.findAllByOrderByBillDateDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Bill> getBillById(Long id) {
        return billRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Bill> getBillByNumber(String billNumber) {
        return billRepository.findByBillNumber(billNumber);
    }

    @Transactional(readOnly = true)
    public List<Bill> searchAndFilterBills(String query, BillStatus status) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return billRepository.searchAndFilterBills(cleanQuery, status);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsForBill(Long billId) {
        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean existsBillForAppointment(Long appointmentId) {
        return billRepository.existsByAppointmentId(appointmentId);
    }

    @Transactional(readOnly = true)
    public boolean existsBillForAdmission(Long admissionId) {
        return billRepository.existsByAdmissionId(admissionId);
    }

    // Dashboard Statistics
    @Transactional(readOnly = true)
    public long getTotalBillsCount() {
        return billRepository.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBilledAmount() {
        return billRepository.sumTotalBilledAmount().setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCollectedAmount() {
        return billRepository.sumTotalCollectedAmount().setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingBalance() {
        return billRepository.sumTotalOutstandingBalance().setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public long getUnpaidBillsCount() {
        return billRepository.countByStatus(BillStatus.UNPAID);
    }

    @Transactional(readOnly = true)
    public long getPartiallyPaidBillsCount() {
        return billRepository.countByStatus(BillStatus.PARTIALLY_PAID);
    }

    @Transactional(readOnly = true)
    public long getPaidBillsCount() {
        return billRepository.countByStatus(BillStatus.PAID);
    }
}
