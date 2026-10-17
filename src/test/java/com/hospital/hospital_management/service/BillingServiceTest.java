package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillRepository billRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @InjectMocks
    private BillingService billingService;

    private Patient samplePatient;
    private Doctor sampleDoctor;
    private Appointment sampleAppointment;
    private Bed sampleBed;
    private Admission sampleAdmission;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient("Amit", "Patel", 30, "Male", "O+",
                "9876543210", "amit@example.com", "Address", "None");
        samplePatient.setId(1L);

        sampleDoctor = new Doctor("Rohan", "Mehra", "Cardiologist", "Cardiology",
                "9876500000", "rohan@hospital.com", 700.0, "Mon-Fri");
        sampleDoctor.setId(2L);

        sampleAppointment = new Appointment(samplePatient, sampleDoctor, LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(10, 30), "Regular checkup");
        sampleAppointment.setId(3L);

        sampleBed = new Bed("B-101", "General Ward A", BedType.GENERAL, "1st Floor",
                new BigDecimal("500.00"), "Near nurse station");
        sampleBed.setId(4L);

        sampleAdmission = new Admission(samplePatient, sampleBed, sampleDoctor,
                LocalDateTime.now().minusDays(3), "Observation");
        sampleAdmission.setId(5L);
        sampleAdmission.setDischargeDate(LocalDateTime.now());
        sampleAdmission.setStatus(AdmissionStatus.DISCHARGED);
    }

    @Test
    void createManualBill_success() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> {
            Bill b = i.getArgument(0);
            b.setId(10L);
            return b;
        });

        List<BillItem> items = Arrays.asList(
                new BillItem("Pharmacy - Antibiotics", 2, new BigDecimal("150.00")),
                new BillItem("Lab - Blood Test", 1, new BigDecimal("400.00"))
        );

        Bill bill = billingService.createManualBill(1L, items, new BigDecimal("50.00"), new BigDecimal("35.00"), "Cash payment requested");

        assertNotNull(bill);
        assertEquals(new BigDecimal("700.00"), bill.getSubtotal()); // 2*150 + 1*400 = 700
        assertEquals(new BigDecimal("50.00"), bill.getDiscountAmount());
        assertEquals(new BigDecimal("35.00"), bill.getTaxAmount());
        assertEquals(new BigDecimal("685.00"), bill.getTotalAmount()); // 700 - 50 + 35 = 685
        assertEquals(new BigDecimal("685.00"), bill.getBalanceAmount());
        assertEquals(BillStatus.UNPAID, bill.getStatus());

        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void createManualBill_noItems_throwsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));

        assertThrows(IllegalArgumentException.class, () ->
                billingService.createManualBill(1L, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, ""));
    }

    @Test
    void createManualBill_negativePrice_throwsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));

        List<BillItem> items = Collections.singletonList(
                new BillItem("Invalid Item", 1, new BigDecimal("-50.00"))
        );

        assertThrows(IllegalArgumentException.class, () ->
                billingService.createManualBill(1L, items, BigDecimal.ZERO, BigDecimal.ZERO, ""));
    }

    @Test
    void createBillForAppointment_success() {
        when(appointmentRepository.findById(3L)).thenReturn(Optional.of(sampleAppointment));
        when(billRepository.existsByAppointmentId(3L)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> {
            Bill b = i.getArgument(0);
            b.setId(15L);
            return b;
        });

        Bill bill = billingService.createBillForAppointment(3L, BigDecimal.ZERO, BigDecimal.ZERO, "Appointment bill");

        assertNotNull(bill);
        assertEquals(new BigDecimal("700.00"), bill.getTotalAmount());
        assertEquals(new BigDecimal("700.00"), bill.getBalanceAmount());
        assertEquals(1, bill.getItems().size());
        assertTrue(bill.getItems().get(0).getDescription().contains("Dr. Rohan Mehra"));
        assertEquals(new BigDecimal("700.00"), bill.getItems().get(0).getLineTotal());

        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void createBillForAppointment_duplicate_throwsException() {
        when(appointmentRepository.findById(3L)).thenReturn(Optional.of(sampleAppointment));
        when(billRepository.existsByAppointmentId(3L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                billingService.createBillForAppointment(3L, BigDecimal.ZERO, BigDecimal.ZERO, ""));

        assertTrue(ex.getMessage().contains("already been generated"));
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void createBillForAdmission_discharged_calculatesStayDaysAndBedRate() {
        when(admissionRepository.findById(5L)).thenReturn(Optional.of(sampleAdmission));
        when(billRepository.existsByAdmissionId(5L)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> {
            Bill b = i.getArgument(0);
            b.setId(20L);
            return b;
        });

        Bill bill = billingService.createBillForAdmission(5L, BigDecimal.ZERO, BigDecimal.ZERO, "Final invoice");

        assertNotNull(bill);
        assertFalse(bill.isPreliminary());
        // 3 days stay @ 500 = 1500 + Doctor care @ 700 = 2200
        assertEquals(new BigDecimal("2200.00"), bill.getSubtotal());
        assertEquals(new BigDecimal("2200.00"), bill.getTotalAmount());

        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void createBillForAdmission_sameDay_minimumOneDayCharged() {
        sampleAdmission.setAdmissionDate(LocalDateTime.now().minusHours(4));
        sampleAdmission.setDischargeDate(LocalDateTime.now());
        sampleAdmission.setAttendingDoctor(null); // No doctor fee

        when(admissionRepository.findById(5L)).thenReturn(Optional.of(sampleAdmission));
        when(billRepository.existsByAdmissionId(5L)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        Bill bill = billingService.createBillForAdmission(5L, BigDecimal.ZERO, BigDecimal.ZERO, "");

        // Same day: 1 day minimum charged @ 500 = 500
        assertEquals(new BigDecimal("500.00"), bill.getSubtotal());
        assertEquals(new BigDecimal("500.00"), bill.getTotalAmount());
    }

    @Test
    void createBillForAdmission_activeStay_marksPreliminary() {
        sampleAdmission.setStatus(AdmissionStatus.ACTIVE);
        sampleAdmission.setDischargeDate(null);
        sampleAdmission.setAdmissionDate(LocalDateTime.now().minusDays(2));

        when(admissionRepository.findById(5L)).thenReturn(Optional.of(sampleAdmission));
        when(billRepository.existsByAdmissionId(5L)).thenReturn(false);
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));

        Bill bill = billingService.createBillForAdmission(5L, BigDecimal.ZERO, BigDecimal.ZERO, "");

        assertTrue(bill.isPreliminary());
        verify(billRepository, times(1)).save(any(Bill.class));
    }

    @Test
    void createBillForAdmission_duplicate_throwsException() {
        when(admissionRepository.findById(5L)).thenReturn(Optional.of(sampleAdmission));
        when(billRepository.existsByAdmissionId(5L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                billingService.createBillForAdmission(5L, BigDecimal.ZERO, BigDecimal.ZERO, ""));

        assertTrue(ex.getMessage().contains("already been generated"));
        verify(billRepository, never()).save(any(Bill.class));
    }

    @Test
    void recordPayment_partialPayment_marksPartiallyPaid() {
        Bill bill = new Bill("INV-TEST-001", samplePatient, LocalDateTime.now());
        bill.setSubtotal(new BigDecimal("1000.00"));
        bill.setTotalAmount(new BigDecimal("1000.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(new BigDecimal("1000.00"));
        bill.setStatus(BillStatus.UNPAID);

        when(billRepository.findByIdWithLock(1L)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        Payment payment = billingService.recordPayment(1L, new BigDecimal("400.00"), PaymentMethod.UPI, "UPI12345", "Partial advance");

        assertNotNull(payment);
        assertEquals(new BigDecimal("400.00"), payment.getAmount());
        assertEquals(new BigDecimal("400.00"), bill.getPaidAmount());
        assertEquals(new BigDecimal("600.00"), bill.getBalanceAmount());
        assertEquals(BillStatus.PARTIALLY_PAID, bill.getStatus());

        verify(billRepository, times(1)).save(bill);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void recordPayment_fullPayment_marksPaid() {
        Bill bill = new Bill("INV-TEST-002", samplePatient, LocalDateTime.now());
        bill.setSubtotal(new BigDecimal("500.00"));
        bill.setTotalAmount(new BigDecimal("500.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(new BigDecimal("500.00"));

        when(billRepository.findByIdWithLock(2L)).thenReturn(Optional.of(bill));
        when(billRepository.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        Payment payment = billingService.recordPayment(2L, new BigDecimal("500.00"), PaymentMethod.CASH, null, "Full settlement");

        assertNotNull(payment);
        assertEquals(new BigDecimal("500.00"), bill.getPaidAmount());
        assertEquals(new BigDecimal("0.00"), bill.getBalanceAmount());
        assertEquals(BillStatus.PAID, bill.getStatus());
    }

    @Test
    void recordPayment_zeroOrNegativeAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                billingService.recordPayment(1L, BigDecimal.ZERO, PaymentMethod.CASH, null, ""));

        assertThrows(IllegalArgumentException.class, () ->
                billingService.recordPayment(1L, new BigDecimal("-100.00"), PaymentMethod.CASH, null, ""));
    }

    @Test
    void recordPayment_exceedsBalance_throwsException() {
        Bill bill = new Bill("INV-TEST-003", samplePatient, LocalDateTime.now());
        bill.setTotalAmount(new BigDecimal("300.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(new BigDecimal("300.00"));

        when(billRepository.findByIdWithLock(3L)).thenReturn(Optional.of(bill));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                billingService.recordPayment(3L, new BigDecimal("350.00"), PaymentMethod.CARD, null, ""));

        assertTrue(ex.getMessage().contains("exceeds outstanding balance"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void recordPayment_duplicateTransactionReference_throwsException() {
        Bill bill = new Bill("INV-TEST-004", samplePatient, LocalDateTime.now());
        bill.setTotalAmount(new BigDecimal("500.00"));
        bill.setPaidAmount(BigDecimal.ZERO);
        bill.setBalanceAmount(new BigDecimal("500.00"));

        when(billRepository.findByIdWithLock(4L)).thenReturn(Optional.of(bill));
        when(paymentRepository.existsByTransactionReference("UPI-DUPLICATE-999")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                billingService.recordPayment(4L, new BigDecimal("200.00"), PaymentMethod.UPI, "UPI-DUPLICATE-999", ""));

        assertTrue(ex.getMessage().contains("has already been recorded"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void dashboardBillingMetrics() {
        when(billRepository.count()).thenReturn(15L);
        when(billRepository.sumTotalBilledAmount()).thenReturn(new BigDecimal("25000.00"));
        when(billRepository.sumTotalCollectedAmount()).thenReturn(new BigDecimal("18000.00"));
        when(billRepository.sumTotalOutstandingBalance()).thenReturn(new BigDecimal("7000.00"));
        when(billRepository.countByStatus(BillStatus.UNPAID)).thenReturn(5L);
        when(billRepository.countByStatus(BillStatus.PARTIALLY_PAID)).thenReturn(3L);
        when(billRepository.countByStatus(BillStatus.PAID)).thenReturn(7L);

        assertEquals(15L, billingService.getTotalBillsCount());
        assertEquals(new BigDecimal("25000.00"), billingService.getTotalBilledAmount());
        assertEquals(new BigDecimal("18000.00"), billingService.getTotalCollectedAmount());
        assertEquals(new BigDecimal("7000.00"), billingService.getTotalOutstandingBalance());
        assertEquals(5L, billingService.getUnpaidBillsCount());
        assertEquals(3L, billingService.getPartiallyPaidBillsCount());
        assertEquals(7L, billingService.getPaidBillsCount());
    }
}
