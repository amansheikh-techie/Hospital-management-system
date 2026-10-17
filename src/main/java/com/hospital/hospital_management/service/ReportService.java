package com.hospital.hospital_management.service;

import com.hospital.hospital_management.dto.report.AppointmentReportData;
import com.hospital.hospital_management.dto.report.BedOccupancyReportData;
import com.hospital.hospital_management.dto.report.BedTypeSummary;
import com.hospital.hospital_management.dto.report.FinancialReportData;
import com.hospital.hospital_management.dto.report.PatientReportData;
import com.hospital.hospital_management.dto.report.WardOccupancySummary;
import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;
import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedStatus;
import com.hospital.hospital_management.model.BedType;
import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;
import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.repository.AppointmentRepository;
import com.hospital.hospital_management.repository.BedRepository;
import com.hospital.hospital_management.repository.BillRepository;
import com.hospital.hospital_management.repository.PatientRepository;
import com.hospital.hospital_management.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service providing report aggregation and calculation for appointments,
 * patients, financial transactions, and bed occupancy.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final BedRepository bedRepository;

    @Autowired
    public ReportService(AppointmentRepository appointmentRepository,
                         PatientRepository patientRepository,
                         BillRepository billRepository,
                         PaymentRepository paymentRepository,
                         BedRepository bedRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.bedRepository = bedRepository;
    }

    /**
     * Aggregates appointment metrics and filters records based on date range, doctor, and status.
     */
    public AppointmentReportData getAppointmentReport(LocalDate startDate, LocalDate endDate, Long doctorId, AppointmentStatus status) {
        validateDateRange(startDate, endDate);

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        List<Appointment> appointments = appointmentRepository.findAppointmentsForReport(
                effectiveStart, effectiveEnd, doctorId, status);

        AppointmentReportData report = new AppointmentReportData();
        report.setStartDate(effectiveStart);
        report.setEndDate(effectiveEnd);
        report.setDoctorId(doctorId);
        report.setStatus(status);
        report.setAppointments(appointments);
        report.setTotalAppointments(appointments.size());

        long scheduled = 0;
        long completed = 0;
        long cancelled = 0;
        for (Appointment a : appointments) {
            if (a.getStatus() == AppointmentStatus.SCHEDULED) scheduled++;
            else if (a.getStatus() == AppointmentStatus.COMPLETED) completed++;
            else if (a.getStatus() == AppointmentStatus.CANCELLED) cancelled++;
        }
        report.setScheduledCount(scheduled);
        report.setCompletedCount(completed);
        report.setCancelledCount(cancelled);

        return report;
    }

    /**
     * Aggregates patient registrations and demographical breakdown over a date range.
     */
    public PatientReportData getPatientReport(LocalDate startDate, LocalDate endDate, String bloodGroup, String gender) {
        validateDateRange(startDate, endDate);

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        String bgFilter = (bloodGroup != null && !bloodGroup.trim().isEmpty()) ? bloodGroup.trim() : null;
        String genderFilter = (gender != null && !gender.trim().isEmpty()) ? gender.trim() : null;

        List<Patient> patients = patientRepository.findPatientsForReport(effectiveStart, effectiveEnd, bgFilter, genderFilter);

        PatientReportData report = new PatientReportData();
        report.setStartDate(effectiveStart);
        report.setEndDate(effectiveEnd);
        report.setBloodGroup(bgFilter);
        report.setGender(genderFilter);
        report.setPatients(patients);
        report.setTotalPatients(patients.size());

        Map<String, Long> genderBreakdown = new LinkedHashMap<>();
        Map<String, Long> bloodGroupBreakdown = new LinkedHashMap<>();

        for (Patient p : patients) {
            String g = (p.getGender() != null && !p.getGender().isBlank()) ? p.getGender() : "Not Specified";
            genderBreakdown.put(g, genderBreakdown.getOrDefault(g, 0L) + 1);

            String bg = (p.getBloodGroup() != null && !p.getBloodGroup().isBlank()) ? p.getBloodGroup() : "Unknown";
            bloodGroupBreakdown.put(bg, bloodGroupBreakdown.getOrDefault(bg, 0L) + 1);
        }
        report.setGenderBreakdown(genderBreakdown);
        report.setBloodGroupBreakdown(bloodGroupBreakdown);

        return report;
    }

    /**
     * Aggregates financial billing and payment data over a date range.
     */
    public FinancialReportData getFinancialReport(LocalDate startDate, LocalDate endDate, PaymentMethod paymentMethod) {
        validateDateRange(startDate, endDate);

        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.now();

        LocalDateTime startDateTime = effectiveStart.atStartOfDay();
        LocalDateTime endDateTime = effectiveEnd.atTime(LocalTime.MAX);

        List<Payment> payments = paymentRepository.findPaymentsForReport(startDateTime, endDateTime, paymentMethod);
        List<Bill> bills = billRepository.findBillsForReport(startDateTime, endDateTime);

        FinancialReportData report = new FinancialReportData();
        report.setStartDate(effectiveStart);
        report.setEndDate(effectiveEnd);
        report.setPaymentMethod(paymentMethod);
        report.setPayments(payments);
        report.setBills(bills);
        report.setTotalPaymentsCount(payments.size());
        report.setTotalBillsCount(bills.size());

        BigDecimal totalBilled = bills.stream()
                .map(Bill::getTotalAmount)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = payments.stream()
                .map(Payment::getAmount)
                .filter(p -> p != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutstanding = bills.stream()
                .map(Bill::getBalanceAmount)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        report.setTotalBilled(totalBilled);
        report.setTotalCollected(totalCollected);
        report.setTotalOutstanding(totalOutstanding);

        Map<PaymentMethod, BigDecimal> methodAmount = new LinkedHashMap<>();
        Map<PaymentMethod, Long> methodCount = new LinkedHashMap<>();

        for (Payment p : payments) {
            PaymentMethod pm = p.getPaymentMethod();
            if (pm != null) {
                methodAmount.put(pm, methodAmount.getOrDefault(pm, BigDecimal.ZERO).add(p.getAmount()));
                methodCount.put(pm, methodCount.getOrDefault(pm, 0L) + 1);
            }
        }
        report.setPaymentMethodAmountBreakdown(methodAmount);
        report.setPaymentMethodCountBreakdown(methodCount);

        return report;
    }

    /**
     * Aggregates bed status, ward occupancy, and bed-type summary.
     */
    public BedOccupancyReportData getBedOccupancyReport() {
        List<Bed> beds = bedRepository.findAll();

        BedOccupancyReportData report = new BedOccupancyReportData();
        report.setBeds(beds);
        report.setTotalBeds(beds.size());

        long available = 0;
        long occupied = 0;
        long maintenance = 0;

        for (Bed b : beds) {
            if (b.getStatus() == BedStatus.AVAILABLE) available++;
            else if (b.getStatus() == BedStatus.OCCUPIED) occupied++;
            else if (b.getStatus() == BedStatus.MAINTENANCE) maintenance++;
        }
        report.setAvailableBeds(available);
        report.setOccupiedBeds(occupied);
        report.setMaintenanceBeds(maintenance);

        double rate = beds.isEmpty() ? 0.0 : Math.round(((double) occupied / beds.size()) * 1000.0) / 10.0;
        report.setOverallOccupancyRate(rate);

        // Group by Ward
        Map<String, List<Bed>> wardMap = beds.stream()
                .collect(Collectors.groupingBy(Bed::getWard, LinkedHashMap::new, Collectors.toList()));

        List<WardOccupancySummary> wardSummaries = new ArrayList<>();
        wardMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String ward = entry.getKey();
                    List<Bed> wardBeds = entry.getValue();
                    long wTotal = wardBeds.size();
                    long wAvail = wardBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
                    long wOcc = wardBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();
                    long wMaint = wardBeds.stream().filter(b -> b.getStatus() == BedStatus.MAINTENANCE).count();
                    wardSummaries.add(new WardOccupancySummary(ward, wTotal, wAvail, wOcc, wMaint));
                });
        report.setWardSummaries(wardSummaries);

        // Group by Bed Type
        Map<BedType, List<Bed>> typeMap = beds.stream()
                .collect(Collectors.groupingBy(Bed::getBedType, LinkedHashMap::new, Collectors.toList()));

        List<BedTypeSummary> typeSummaries = new ArrayList<>();
        for (BedType type : BedType.values()) {
            List<Bed> typeBeds = typeMap.getOrDefault(type, List.of());
            long tTotal = typeBeds.size();
            long tAvail = typeBeds.stream().filter(b -> b.getStatus() == BedStatus.AVAILABLE).count();
            long tOcc = typeBeds.stream().filter(b -> b.getStatus() == BedStatus.OCCUPIED).count();

            BigDecimal avgCharge = BigDecimal.ZERO;
            if (!typeBeds.isEmpty()) {
                BigDecimal sum = typeBeds.stream()
                        .map(Bed::getDailyCharge)
                        .filter(c -> c != null)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                avgCharge = sum.divide(BigDecimal.valueOf(typeBeds.size()), 2, RoundingMode.HALF_UP);
            }
            typeSummaries.add(new BedTypeSummary(type, tTotal, tAvail, tOcc, avgCharge));
        }
        report.setTypeSummaries(typeSummaries);

        return report;
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }
}
