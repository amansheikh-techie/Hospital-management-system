package com.hospital.hospital_management.service;

import com.hospital.hospital_management.dto.report.AppointmentReportData;
import com.hospital.hospital_management.dto.report.BedOccupancyReportData;
import com.hospital.hospital_management.dto.report.FinancialReportData;
import com.hospital.hospital_management.dto.report.PatientReportData;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BedRepository bedRepository;

    @InjectMocks
    private ReportService reportService;

    private LocalDate today;
    private LocalDate thirtyDaysAgo;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        thirtyDaysAgo = today.minusDays(30);
    }

    @Test
    void testAppointmentReportCalculations() {
        Appointment a1 = new Appointment();
        a1.setStatus(AppointmentStatus.SCHEDULED);

        Appointment a2 = new Appointment();
        a2.setStatus(AppointmentStatus.COMPLETED);

        Appointment a3 = new Appointment();
        a3.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findAppointmentsForReport(thirtyDaysAgo, today, null, null))
                .thenReturn(List.of(a1, a2, a3));

        AppointmentReportData report = reportService.getAppointmentReport(thirtyDaysAgo, today, null, null);

        assertNotNull(report);
        assertEquals(3, report.getTotalAppointments());
        assertEquals(1, report.getScheduledCount());
        assertEquals(1, report.getCompletedCount());
        assertEquals(1, report.getCancelledCount());
    }

    @Test
    void testAppointmentReportInvalidDateRangeThrowsException() {
        LocalDate start = LocalDate.of(2026, 5, 20);
        LocalDate end = LocalDate.of(2026, 5, 10);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reportService.getAppointmentReport(start, end, null, null)
        );
        assertTrue(ex.getMessage().contains("Start date cannot be after end date"));
    }

    @Test
    void testPatientReportCalculations() {
        Patient p1 = new Patient();
        p1.setGender("Male");
        p1.setBloodGroup("O+");

        Patient p2 = new Patient();
        p2.setGender("Female");
        p2.setBloodGroup("A+");

        Patient p3 = new Patient();
        p3.setGender("Female");
        p3.setBloodGroup("O+");

        when(patientRepository.findPatientsForReport(thirtyDaysAgo, today, null, null))
                .thenReturn(List.of(p1, p2, p3));

        PatientReportData report = reportService.getPatientReport(thirtyDaysAgo, today, null, null);

        assertNotNull(report);
        assertEquals(3, report.getTotalPatients());
        assertEquals(2L, report.getGenderBreakdown().get("Female"));
        assertEquals(1L, report.getGenderBreakdown().get("Male"));
        assertEquals(2L, report.getBloodGroupBreakdown().get("O+"));
        assertEquals(1L, report.getBloodGroupBreakdown().get("A+"));
    }

    @Test
    void testFinancialReportCalculations() {
        Bill b1 = new Bill();
        b1.setTotalAmount(new BigDecimal("1500.00"));
        b1.setPaidAmount(new BigDecimal("1000.00"));
        b1.setBalanceAmount(new BigDecimal("500.00"));

        Bill b2 = new Bill();
        b2.setTotalAmount(new BigDecimal("500.00"));
        b2.setPaidAmount(new BigDecimal("500.00"));
        b2.setBalanceAmount(BigDecimal.ZERO);

        Payment p1 = new Payment();
        p1.setAmount(new BigDecimal("1000.00"));
        p1.setPaymentMethod(PaymentMethod.UPI);

        Payment p2 = new Payment();
        p2.setAmount(new BigDecimal("500.00"));
        p2.setPaymentMethod(PaymentMethod.CASH);

        when(billRepository.findBillsForReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(b1, b2));
        when(paymentRepository.findPaymentsForReport(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                .thenReturn(List.of(p1, p2));

        FinancialReportData report = reportService.getFinancialReport(thirtyDaysAgo, today, null);

        assertNotNull(report);
        assertEquals(new BigDecimal("2000.00"), report.getTotalBilled());
        assertEquals(new BigDecimal("1500.00"), report.getTotalCollected());
        assertEquals(new BigDecimal("500.00"), report.getTotalOutstanding());
        assertEquals(2, report.getTotalBillsCount());
        assertEquals(2, report.getTotalPaymentsCount());
        assertEquals(new BigDecimal("1000.00"), report.getPaymentMethodAmountBreakdown().get(PaymentMethod.UPI));
        assertEquals(new BigDecimal("500.00"), report.getPaymentMethodAmountBreakdown().get(PaymentMethod.CASH));
    }

    @Test
    void testBedOccupancyReportCalculations() {
        Bed b1 = new Bed();
        b1.setBedNumber("B1");
        b1.setWard("ICU");
        b1.setBedType(BedType.ICU);
        b1.setStatus(BedStatus.OCCUPIED);
        b1.setDailyCharge(new BigDecimal("2000.00"));

        Bed b2 = new Bed();
        b2.setBedNumber("B2");
        b2.setWard("ICU");
        b2.setBedType(BedType.ICU);
        b2.setStatus(BedStatus.AVAILABLE);
        b2.setDailyCharge(new BigDecimal("2000.00"));

        Bed b3 = new Bed();
        b3.setBedNumber("B3");
        b3.setWard("General");
        b3.setBedType(BedType.GENERAL);
        b3.setStatus(BedStatus.MAINTENANCE);
        b3.setDailyCharge(new BigDecimal("500.00"));

        when(bedRepository.findAll()).thenReturn(List.of(b1, b2, b3));

        BedOccupancyReportData report = reportService.getBedOccupancyReport();

        assertNotNull(report);
        assertEquals(3, report.getTotalBeds());
        assertEquals(1, report.getOccupiedBeds());
        assertEquals(1, report.getAvailableBeds());
        assertEquals(1, report.getMaintenanceBeds());
        assertEquals(33.3, report.getOverallOccupancyRate()); // 1/3 * 100 = 33.3%

        assertEquals(2, report.getWardSummaries().size());
        assertEquals(BedType.values().length, report.getTypeSummaries().size());
    }

    @Test
    void testEmptyReportResultsHandledGracefully() {
        when(appointmentRepository.findAppointmentsForReport(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(patientRepository.findPatientsForReport(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bedRepository.findAll())
                .thenReturn(Collections.emptyList());

        AppointmentReportData apptReport = reportService.getAppointmentReport(thirtyDaysAgo, today, null, null);
        assertEquals(0, apptReport.getTotalAppointments());

        PatientReportData patientReport = reportService.getPatientReport(thirtyDaysAgo, today, null, null);
        assertEquals(0, patientReport.getTotalPatients());

        BedOccupancyReportData bedReport = reportService.getBedOccupancyReport();
        assertEquals(0, bedReport.getTotalBeds());
        assertEquals(0.0, bedReport.getOverallOccupancyRate());
    }
}
