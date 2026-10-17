package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.dto.report.AppointmentReportData;
import com.hospital.hospital_management.dto.report.BedOccupancyReportData;
import com.hospital.hospital_management.dto.report.FinancialReportData;
import com.hospital.hospital_management.dto.report.PatientReportData;
import com.hospital.hospital_management.model.AppointmentStatus;
import com.hospital.hospital_management.model.PaymentMethod;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/**
 * Controller handling hospital analytics, operational reporting, and financial auditing.
 */
@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final DoctorService doctorService;

    @Autowired
    public ReportController(ReportService reportService, DoctorService doctorService) {
        this.reportService = reportService;
        this.doctorService = doctorService;
    }

    /**
     * Reports home hub, redirecting to appointments report by default.
     */
    @GetMapping
    public String index() {
        return "redirect:/reports/appointments";
    }

    /**
     * Appointment report with date range, doctor, and status filters.
     */
    @GetMapping("/appointments")
    public String appointmentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) AppointmentStatus status,
            Model model) {

        model.addAttribute("activeTab", "appointments");
        model.addAttribute("pageTitle", "Appointment Report");
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("selectedStatus", status);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            model.addAttribute("errorMessage", "Start date cannot be after end date.");
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("report", new AppointmentReportData());
            return "reports/appointments";
        }

        AppointmentReportData report = reportService.getAppointmentReport(startDate, endDate, doctorId, status);
        model.addAttribute("report", report);
        model.addAttribute("startDate", report.getStartDate());
        model.addAttribute("endDate", report.getEndDate());

        return "reports/appointments";
    }

    /**
     * Patient registration report with date range, blood group, and gender filters.
     */
    @GetMapping("/patients")
    public String patientReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String gender,
            Model model) {

        model.addAttribute("activeTab", "patients");
        model.addAttribute("pageTitle", "Patient Registration Report");
        model.addAttribute("selectedBloodGroup", bloodGroup);
        model.addAttribute("selectedGender", gender);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            model.addAttribute("errorMessage", "Start date cannot be after end date.");
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("report", new PatientReportData());
            return "reports/patients";
        }

        PatientReportData report = reportService.getPatientReport(startDate, endDate, bloodGroup, gender);
        model.addAttribute("report", report);
        model.addAttribute("startDate", report.getStartDate());
        model.addAttribute("endDate", report.getEndDate());

        return "reports/patients";
    }

    /**
     * Financial and revenue report with date range and payment method filters.
     * Restricted strictly to ADMIN and BILLING_STAFF roles.
     */
    @GetMapping("/financial")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public String financialReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            Model model) {

        model.addAttribute("activeTab", "financial");
        model.addAttribute("pageTitle", "Financial Audit Report");
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("selectedPaymentMethod", paymentMethod);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            model.addAttribute("errorMessage", "Start date cannot be after end date.");
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);
            model.addAttribute("report", new FinancialReportData());
            return "reports/financial";
        }

        FinancialReportData report = reportService.getFinancialReport(startDate, endDate, paymentMethod);
        model.addAttribute("report", report);
        model.addAttribute("startDate", report.getStartDate());
        model.addAttribute("endDate", report.getEndDate());

        return "reports/financial";
    }

    /**
     * Hospital bed occupancy summary report.
     */
    @GetMapping("/beds")
    public String bedOccupancyReport(Model model) {
        model.addAttribute("activeTab", "beds");
        model.addAttribute("pageTitle", "Bed Occupancy Report");
        BedOccupancyReportData report = reportService.getBedOccupancyReport();
        model.addAttribute("report", report);
        return "reports/beds";
    }
}
