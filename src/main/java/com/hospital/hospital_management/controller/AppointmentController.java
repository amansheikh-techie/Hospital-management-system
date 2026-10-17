package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;
import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.service.AppointmentService;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.PatientService;
import com.hospital.hospital_management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing Appointment workflows:
 * Booking, rescheduling, viewing, searching, cancelling, and completing appointments.
 */
@Controller
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final UserService userService;

    @Autowired
    public AppointmentController(AppointmentService appointmentService,
                                 PatientService patientService,
                                 DoctorService doctorService,
                                 UserService userService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.userService = userService;
    }

    /**
     * Display appointments list with optional search and filter criteria.
     * Enforces that authenticated Doctors only view their own schedule.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'BILLING_STAFF')")
    public String listAppointments(
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "status", required = false) AppointmentStatus status,
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model) {

        // Enforce doctor restriction: if logged-in user is a DOCTOR, restrict to their ID or show safe message
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Optional<User> currentUserOpt = userService.getUserByUsername(auth.getName());
            if (currentUserOpt.isPresent() && currentUserOpt.get().getRole() == Role.ROLE_DOCTOR) {
                if (currentUserOpt.get().getDoctor() != null) {
                    doctorId = currentUserOpt.get().getDoctor().getId();
                    model.addAttribute("isDoctorRestricted", true);
                } else {
                    model.addAttribute("appointments", java.util.Collections.emptyList());
                    model.addAttribute("doctors", java.util.Collections.emptyList());
                    model.addAttribute("patients", java.util.Collections.emptyList());
                    model.addAttribute("statuses", AppointmentStatus.values());
                    model.addAttribute("totalCount", 0);
                    model.addAttribute("todayCount", 0);
                    model.addAttribute("warningMessage", "Your Doctor user account is not linked to an active Doctor profile. Please contact an Administrator to link your account to view your scheduled appointments.");
                    return "appointment/list";
                }
            }
        }

        List<Appointment> appointments;
        boolean isFiltered = (doctorId != null || patientId != null || date != null || status != null || (keyword != null && !keyword.trim().isEmpty()));

        if (isFiltered) {
            appointments = appointmentService.filterAppointments(doctorId, patientId, date, status, keyword);
        } else {
            appointments = appointmentService.getAllAppointments();
        }

        model.addAttribute("appointments", appointments);
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("totalCount", appointmentService.getTotalAppointmentCount());
        model.addAttribute("todayCount", appointmentService.getTodayAppointmentCount());
        model.addAttribute("pageTitle", "Appointments");

        // Retain filter state in form
        model.addAttribute("selectedDoctorId", doctorId);
        model.addAttribute("selectedPatientId", patientId);
        model.addAttribute("selectedDate", date);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isFiltered", isFiltered);

        return "appointment/list";
    }

    /**
     * Show booking form for a new appointment.
     */
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String showBookingForm(
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            Model model) {

        Appointment appointment = new Appointment();
        appointment.setAppointmentDate(LocalDate.now());
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));

        if (patientId != null) {
            patientService.getPatientById(patientId).ifPresent(appointment::setPatient);
        }
        if (doctorId != null) {
            doctorService.getDoctorById(doctorId).ifPresent(appointment::setDoctor);
        }

        populateFormModel(model, appointment, "Book New Appointment");
        return "appointment/form";
    }

    /**
     * Process appointment booking submission.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String bookAppointment(
            @Valid @ModelAttribute("appointment") Appointment appointment,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            populateFormModel(model, appointment, "Book New Appointment");
            return "appointment/form";
        }

        try {
            Appointment saved = appointmentService.bookAppointment(appointment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Appointment #" + saved.getId() + " booked successfully for " +
                    saved.getPatient().getFullName() + " with " + saved.getDoctor().getDisplayName() +
                    " on " + saved.getAppointmentDate() + " (" + saved.getStartTime() + " - " + saved.getEndTime() + ").");
            return "redirect:/appointments";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, appointment, "Book New Appointment");
            return "appointment/form";
        }
    }

    /**
     * View detailed appointment information.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'BILLING_STAFF')")
    public String viewAppointmentDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    model.addAttribute("appointment", appointment);
                    model.addAttribute("pageTitle", "Appointment Details");
                    return "appointment/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Appointment with ID " + id + " was not found.");
                    return "redirect:/appointments";
                });
    }

    /**
     * Show form to reschedule or edit an appointment.
     */
    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return appointmentService.getAppointmentById(id)
                .map(appointment -> {
                    populateFormModel(model, appointment, "Reschedule / Edit Appointment #" + id);
                    return "appointment/form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Appointment with ID " + id + " was not found.");
                    return "redirect:/appointments";
                });
    }

    /**
     * Process appointment update / reschedule submission.
     */
    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public String updateAppointment(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute("appointment") Appointment appointment,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            populateFormModel(model, appointment, "Reschedule / Edit Appointment #" + id);
            return "appointment/form";
        }

        try {
            appointmentService.updateAppointment(id, appointment);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Appointment #" + id + " updated successfully!");
            return "redirect:/appointments";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, appointment, "Reschedule / Edit Appointment #" + id);
            return "appointment/form";
        }
    }

    /**
     * Safely cancel an appointment.
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public String cancelAppointment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " has been cancelled.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/appointments";
    }

    /**
     * Mark an appointment as completed.
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST', 'DOCTOR')")
    public String completeAppointment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            appointmentService.completeAppointment(id);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " marked as Completed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/appointments";
    }

    /**
     * Delete an appointment record.
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteAppointment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = appointmentService.deleteAppointment(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " deleted successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Could not delete: Appointment #" + id + " not found.");
        }
        return "redirect:/appointments";
    }

    private void populateFormModel(Model model, Appointment appointment, String pageTitle) {
        model.addAttribute("appointment", appointment);
        model.addAttribute("patients", patientService.getAllPatients());
        model.addAttribute("doctors", doctorService.getAllDoctors());
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("pageTitle", pageTitle);
    }
}
