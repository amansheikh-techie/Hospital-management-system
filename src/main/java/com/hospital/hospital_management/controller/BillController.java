package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Admission;
import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.BillItem;
import com.hospital.hospital_management.model.BillStatus;
import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;
import com.hospital.hospital_management.service.AdmissionService;
import com.hospital.hospital_management.service.AppointmentService;
import com.hospital.hospital_management.service.BillingService;
import com.hospital.hospital_management.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/bills")
public class BillController {

    private final BillingService billingService;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final AdmissionService admissionService;

    @Autowired
    public BillController(BillingService billingService,
                          PatientService patientService,
                          AppointmentService appointmentService,
                          AdmissionService admissionService) {
        this.billingService = billingService;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.admissionService = admissionService;
    }

    /**
     * Lists bills with search and filtering, plus live financial summary counters.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF', 'RECEPTIONIST', 'DOCTOR')")
    public String listBills(@RequestParam(value = "query", required = false) String query,
                            @RequestParam(value = "status", required = false) BillStatus status,
                            Model model) {
        List<Bill> bills = billingService.searchAndFilterBills(query, status);
        model.addAttribute("bills", bills);
        model.addAttribute("billStatuses", BillStatus.values());

        // Live financial metrics
        model.addAttribute("totalBills", billingService.getTotalBillsCount());
        model.addAttribute("totalBilledAmount", billingService.getTotalBilledAmount());
        model.addAttribute("totalCollectedAmount", billingService.getTotalCollectedAmount());
        model.addAttribute("totalOutstandingBalance", billingService.getTotalOutstandingBalance());
        model.addAttribute("unpaidCount", billingService.getUnpaidBillsCount());
        model.addAttribute("partiallyPaidCount", billingService.getPartiallyPaidBillsCount());

        model.addAttribute("query", query);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Billing & Invoices");
        return "billing/list";
    }

    /**
     * Shows form to create a new invoice (manual, or linked to appointment/admission).
     */
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public String showCreateForm(@RequestParam(value = "patientId", required = false) Long patientId,
                                 @RequestParam(value = "appointmentId", required = false) Long appointmentId,
                                 @RequestParam(value = "admissionId", required = false) Long admissionId,
                                 Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("selectedPatientId", patientId);
        model.addAttribute("selectedAppointmentId", appointmentId);
        model.addAttribute("selectedAdmissionId", admissionId);

        if (appointmentId != null) {
            Optional<Appointment> apptOpt = appointmentService.getAppointmentById(appointmentId);
            apptOpt.ifPresent(appt -> {
                model.addAttribute("linkedAppointment", appt);
                if (patientId == null) {
                    model.addAttribute("selectedPatientId", appt.getPatient().getId());
                }
            });
        }

        if (admissionId != null) {
            Optional<Admission> admOpt = admissionService.getAdmissionById(admissionId);
            admOpt.ifPresent(adm -> {
                model.addAttribute("linkedAdmission", adm);
                if (patientId == null) {
                    model.addAttribute("selectedPatientId", adm.getPatient().getId());
                }
            });
        }

        model.addAttribute("pageTitle", "Generate Invoice");
        return "billing/form";
    }

    /**
     * Quick-generate invoice from an Appointment.
     */
    @PostMapping("/from-appointment")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public String generateFromAppointment(@RequestParam("appointmentId") Long appointmentId,
                                          @RequestParam(value = "discountAmount", required = false) BigDecimal discountAmount,
                                          @RequestParam(value = "taxAmount", required = false) BigDecimal taxAmount,
                                          @RequestParam(value = "notes", required = false) String notes,
                                          RedirectAttributes redirectAttributes) {
        try {
            Bill bill = billingService.createBillForAppointment(appointmentId, discountAmount, taxAmount, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invoice " + bill.getBillNumber() + " generated successfully for Appointment #" + appointmentId + ".");
            return "redirect:/bills/" + bill.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/appointments/" + appointmentId;
        }
    }

    /**
     * Quick-generate invoice from an Admission.
     */
    @PostMapping("/from-admission")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public String generateFromAdmission(@RequestParam("admissionId") Long admissionId,
                                        @RequestParam(value = "discountAmount", required = false) BigDecimal discountAmount,
                                        @RequestParam(value = "taxAmount", required = false) BigDecimal taxAmount,
                                        @RequestParam(value = "notes", required = false) String notes,
                                        RedirectAttributes redirectAttributes) {
        try {
            Bill bill = billingService.createBillForAdmission(admissionId, discountAmount, taxAmount, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invoice " + bill.getBillNumber() + " generated successfully for Admission #" + admissionId + ".");
            return "redirect:/bills/" + bill.getId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admissions/" + admissionId;
        }
    }

    /**
     * Handles manual bill creation with custom line items.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF')")
    public String createManualBill(@RequestParam(value = "patientId", required = false) Long patientId,
                                   @RequestParam(value = "itemDescriptions", required = false) List<String> descriptions,
                                   @RequestParam(value = "itemQuantities", required = false) List<Integer> quantities,
                                   @RequestParam(value = "itemUnitPrices", required = false) List<BigDecimal> unitPrices,
                                   @RequestParam(value = "discountAmount", required = false) BigDecimal discountAmount,
                                   @RequestParam(value = "taxAmount", required = false) BigDecimal taxAmount,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (patientId == null) {
            model.addAttribute("errorMessage", "Please select a patient.");
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("pageTitle", "Generate Invoice");
            return "billing/form";
        }

        List<BillItem> items = new ArrayList<>();
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                String desc = descriptions.get(i);
                if (desc != null && !desc.trim().isEmpty()) {
                    Integer qty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ?
                            quantities.get(i) : 1;
                    BigDecimal price = (unitPrices != null && i < unitPrices.size() && unitPrices.get(i) != null) ?
                            unitPrices.get(i) : BigDecimal.ZERO;
                    items.add(new BillItem(desc.trim(), qty, price));
                }
            }
        }

        if (items.isEmpty()) {
            model.addAttribute("errorMessage", "Please add at least one line item with description and price.");
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("selectedPatientId", patientId);
            model.addAttribute("pageTitle", "Generate Invoice");
            return "billing/form";
        }

        try {
            Bill bill = billingService.createBill(patientId, items, discountAmount, taxAmount, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Invoice " + bill.getBillNumber() + " created successfully.");
            return "redirect:/bills/" + bill.getId();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("patients", patientService.getAllPatients());
            model.addAttribute("selectedPatientId", patientId);
            model.addAttribute("pageTitle", "Generate Invoice");
            return "billing/form";
        }
    }

    /**
     * Views bill details including line items, payments history, balance, and settlement status.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF', 'RECEPTIONIST', 'DOCTOR')")
    public String viewBill(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Bill> billOpt = billingService.getBillById(id);
        if (billOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice not found with ID: " + id);
            return "redirect:/bills";
        }

        Bill bill = billOpt.get();
        List<Payment> payments = billingService.getPaymentsForBill(id);

        model.addAttribute("bill", bill);
        model.addAttribute("payments", payments);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("pageTitle", "Invoice " + bill.getBillNumber());
        return "billing/view";
    }

    /**
     * Printable view of an invoice for patient receipt / record keeping.
     */
    @GetMapping("/{id}/print")
    @PreAuthorize("hasAnyRole('ADMIN', 'BILLING_STAFF', 'RECEPTIONIST', 'DOCTOR')")
    public String printBill(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Bill> billOpt = billingService.getBillById(id);
        if (billOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice not found with ID: " + id);
            return "redirect:/bills";
        }

        Bill bill = billOpt.get();
        List<Payment> payments = billingService.getPaymentsForBill(id);

        model.addAttribute("bill", bill);
        model.addAttribute("payments", payments);
        model.addAttribute("pageTitle", "Receipt – " + bill.getBillNumber());
        return "billing/print";
    }
}
