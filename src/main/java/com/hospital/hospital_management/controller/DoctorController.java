package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for managing Doctor workflows:
 * Registration, viewing, searching, editing, and deletion.
 */
@Controller
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @Autowired
    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    /**
     * Display list of doctors with optional search filter.
     */
    @GetMapping
    public String listDoctors(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        List<Doctor> doctors;
        if (keyword != null && !keyword.trim().isEmpty()) {
            doctors = doctorService.searchDoctors(keyword);
            model.addAttribute("keyword", keyword.trim());
        } else {
            doctors = doctorService.getAllDoctors();
        }
        model.addAttribute("doctors", doctors);
        model.addAttribute("totalCount", doctorService.getTotalDoctorCount());
        return "doctor/list";
    }

    /**
     * Show registration form for a new doctor.
     */
    @GetMapping("/new")
    public String showRegistrationForm(Model model) {
        model.addAttribute("doctor", new Doctor());
        model.addAttribute("pageTitle", "Add New Doctor");
        return "doctor/form";
    }

    /**
     * Process new doctor registration.
     */
    @PostMapping
    public String registerDoctor(@Valid @ModelAttribute("doctor") Doctor doctor,
                                 BindingResult result,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Add New Doctor");
            return "doctor/form";
        }

        doctorService.saveDoctor(doctor);
        redirectAttributes.addFlashAttribute("successMessage",
                "Doctor " + doctor.getDisplayName() + " registered successfully! (ID: " + doctor.getId() + ")");
        return "redirect:/doctors";
    }

    /**
     * View complete details for a specific doctor.
     */
    @GetMapping("/{id}")
    public String viewDoctorDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return doctorService.getDoctorById(id)
                .map(doctor -> {
                    model.addAttribute("doctor", doctor);
                    return "doctor/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Doctor with ID " + id + " was not found.");
                    return "redirect:/doctors";
                });
    }

    /**
     * Show form to edit an existing doctor.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return doctorService.getDoctorById(id)
                .map(doctor -> {
                    model.addAttribute("doctor", doctor);
                    model.addAttribute("pageTitle", "Edit " + doctor.getDisplayName());
                    return "doctor/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Doctor with ID " + id + " was not found.");
                    return "redirect:/doctors";
                });
    }

    /**
     * Process doctor update submission.
     */
    @PostMapping("/{id}/edit")
    public String updateDoctor(@PathVariable("id") Long id,
                               @Valid @ModelAttribute("doctor") Doctor doctor,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Doctor #" + id);
            return "doctor/form";
        }

        try {
            doctorService.updateDoctor(id, doctor);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Doctor " + doctor.getDisplayName() + " updated successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/doctors";
    }

    /**
     * Safely delete a doctor record.
     */
    @PostMapping("/{id}/delete")
    public String deleteDoctor(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = doctorService.deleteDoctor(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Doctor #" + id + " deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete: Doctor #" + id + " not found.");
        }
        return "redirect:/doctors";
    }
}
