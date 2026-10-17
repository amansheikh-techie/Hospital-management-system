package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing Patient workflows:
 * Registration, viewing, searching, editing, and deletion.
 */
@Controller
@RequestMapping("/patients")
public class PatientController {

    private final PatientService patientService;

    @Autowired
    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Display list of all patients with optional search filtering.
     */
    @GetMapping
    public String listPatients(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Patient> patients;
        if (keyword != null && !keyword.trim().isEmpty()) {
            patients = patientService.searchPatients(keyword);
            model.addAttribute("keyword", keyword.trim());
        } else {
            patients = patientService.getAllPatients();
        }
        model.addAttribute("patients", patients);
        model.addAttribute("totalCount", patientService.getTotalPatientCount());
        return "patient/list";
    }

    /**
     * Show registration form for a new patient.
     */
    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        model.addAttribute("patient", new Patient());
        model.addAttribute("pageTitle", "Register New Patient");
        return "patient/form";
    }

    /**
     * Process new patient registration.
     */
    @PostMapping
    public String registerPatient(@Valid @ModelAttribute("patient") Patient patient,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Register New Patient");
            return "patient/form";
        }

        patientService.savePatient(patient);
        redirectAttributes.addFlashAttribute("successMessage",
                "Patient " + patient.getFullName() + " registered successfully! (ID: " + patient.getId() + ")");
        return "redirect:/patients";
    }

    /**
     * View complete details for a specific patient.
     */
    @GetMapping("/{id}")
    public String viewPatientDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return patientService.getPatientById(id)
                .map(patient -> {
                    model.addAttribute("patient", patient);
                    return "patient/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Patient with ID " + id + " was not found.");
                    return "redirect:/patients";
                });
    }

    /**
     * Show form to edit an existing patient.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return patientService.getPatientById(id)
                .map(patient -> {
                    model.addAttribute("patient", patient);
                    model.addAttribute("pageTitle", "Edit Patient #" + id);
                    return "patient/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Patient with ID " + id + " was not found.");
                    return "redirect:/patients";
                });
    }

    /**
     * Process patient update submission.
     */
    @PostMapping("/{id}/edit")
    public String updatePatient(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("patient") Patient patient,
                                BindingResult result,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Patient #" + id);
            return "patient/form";
        }

        try {
            patientService.updatePatient(id, patient);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Patient #" + id + " (" + patient.getFullName() + ") updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/patients";
    }

    /**
     * Delete a patient safely.
     */
    @PostMapping("/{id}/delete")
    public String deletePatient(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = patientService.deletePatient(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Patient #" + id + " deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete: Patient #" + id + " not found.");
        }
        return "redirect:/patients";
    }
}
