package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Admission;
import com.hospital.hospital_management.model.AdmissionStatus;
import com.hospital.hospital_management.service.AdmissionService;
import com.hospital.hospital_management.service.BedService;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admissions")
public class AdmissionController {

    private final AdmissionService admissionService;
    private final BedService bedService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    @Autowired
    public AdmissionController(AdmissionService admissionService,
                               BedService bedService,
                               PatientService patientService,
                               DoctorService doctorService) {
        this.admissionService = admissionService;
        this.bedService = bedService;
        this.patientService = patientService;
        this.doctorService = doctorService;
    }

    /**
     * Lists patient admissions with filter by patient name, ward, and status.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'BILLING_STAFF')")
    public String listAdmissions(@RequestParam(value = "patientName", required = false) String patientName,
                                 @RequestParam(value = "status", required = false) AdmissionStatus status,
                                 @RequestParam(value = "ward", required = false) String ward,
                                 Model model) {
        List<Admission> admissions = admissionService.searchAndFilterAdmissions(patientName, status, ward);
        model.addAttribute("admissions", admissions);
        model.addAttribute("wards", bedService.getDistinctWards());
        model.addAttribute("admissionStatuses", AdmissionStatus.values());
        model.addAttribute("activeAdmissionsCount", admissionService.getActiveAdmissionCount());
        model.addAttribute("todayAdmissionsCount", admissionService.getTodayAdmissionCount());

        model.addAttribute("patientName", patientName);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedWard", ward);
        model.addAttribute("pageTitle", "Inpatient Admissions");
        return "admission/list";
    }

    /**
     * Displays form to admit a patient to an available bed.
     */
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String showAdmissionForm(@RequestParam(value = "patientId", required = false) Long patientId,
                                    @RequestParam(value = "bedId", required = false) Long bedId,
                                    Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("availableBeds", bedService.getAvailableBeds());
        model.addAttribute("doctors", doctorService.getAllDoctors());

        model.addAttribute("selectedPatientId", patientId);
        model.addAttribute("selectedBedId", bedId);
        model.addAttribute("defaultAdmissionDate",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("pageTitle", "Admit Inpatient");
        return "admission/form";
    }

    /**
     * Processes admission creation with locking and status updates.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String admitPatient(@RequestParam(value = "patientId", required = false) Long patientId,
                               @RequestParam(value = "bedId", required = false) Long bedId,
                               @RequestParam(value = "doctorId", required = false) Long doctorId,
                               @RequestParam(value = "admissionDate", required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime admissionDate,
                               @RequestParam(value = "admissionReason", required = false) String admissionReason,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (patientId == null) {
            model.addAttribute("errorMessage", "Please select a patient to admit.");
            return populateAndReturnAdmissionForm(patientId, bedId, doctorId, admissionDate, admissionReason, model);
        }
        if (bedId == null) {
            model.addAttribute("errorMessage", "Please select an available hospital bed.");
            return populateAndReturnAdmissionForm(patientId, bedId, doctorId, admissionDate, admissionReason, model);
        }
        if (admissionDate == null) {
            admissionDate = LocalDateTime.now();
        }

        try {
            Admission admission = admissionService.admitPatient(patientId, bedId, doctorId, admissionDate, admissionReason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Patient " + admission.getPatient().getFirstName() + " " + admission.getPatient().getLastName() +
                            " admitted successfully to Bed " + admission.getBed().getBedNumber() +
                            " in " + admission.getBed().getWard() + " (Admission ID: #" + admission.getId() + ").");
            return "redirect:/admissions/" + admission.getId();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return populateAndReturnAdmissionForm(patientId, bedId, doctorId, admissionDate, admissionReason, model);
        }
    }

    /**
     * Views admission details including bed, patient, and discharge history.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'BILLING_STAFF')")
    public String viewAdmission(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Admission> admissionOpt = admissionService.getAdmissionById(id);
        if (admissionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Admission not found with ID: " + id);
            return "redirect:/admissions";
        }

        model.addAttribute("admission", admissionOpt.get());
        model.addAttribute("pageTitle", "Admission Record #" + id);
        return "admission/view";
    }

    /**
     * Shows discharge form.
     */
    @GetMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String showDischargeForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Admission> admissionOpt = admissionService.getAdmissionById(id);
        if (admissionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Admission not found with ID: " + id);
            return "redirect:/admissions";
        }

        Admission admission = admissionOpt.get();
        if (!AdmissionStatus.ACTIVE.equals(admission.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Admission #" + id + " is already marked as " + admission.getStatus().getDisplayName());
            return "redirect:/admissions/" + id;
        }

        model.addAttribute("admission", admission);
        model.addAttribute("defaultDischargeDate",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("pageTitle", "Discharge Patient – Admission #" + id);
        return "admission/discharge";
    }

    /**
     * Handles discharge processing.
     */
    @PostMapping("/{id}/discharge")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String dischargePatient(@PathVariable("id") Long id,
                                   @RequestParam(value = "dischargeDate", required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dischargeDate,
                                   @RequestParam(value = "dischargeNotes", required = false) String dischargeNotes,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        try {
            Admission admission = admissionService.dischargePatient(id, dischargeDate, dischargeNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Patient " + admission.getPatient().getFirstName() + " " + admission.getPatient().getLastName() +
                            " discharged successfully. Bed " + admission.getBed().getBedNumber() +
                            " is now released and AVAILABLE.");
            return "redirect:/admissions/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Optional<Admission> admissionOpt = admissionService.getAdmissionById(id);
            model.addAttribute("admission", admissionOpt.orElse(null));
            model.addAttribute("defaultDischargeDate",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("pageTitle", "Discharge Patient – Admission #" + id);
            return "admission/discharge";
        }
    }

    private String populateAndReturnAdmissionForm(Long patientId, Long bedId, Long doctorId,
                                                  LocalDateTime admissionDate, String admissionReason,
                                                  Model model) {
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("availableBeds", bedService.getAvailableBeds());
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("selectedPatientId", patientId);
        model.addAttribute("selectedBedId", bedId);
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("defaultAdmissionDate", admissionDate != null ?
                admissionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) :
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        model.addAttribute("admissionReason", admissionReason);
        model.addAttribute("pageTitle", "Admit Inpatient");
        return "admission/form";
    }
}
