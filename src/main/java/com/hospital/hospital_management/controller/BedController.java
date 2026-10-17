package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Admission;
import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedStatus;
import com.hospital.hospital_management.model.BedType;
import com.hospital.hospital_management.service.BedService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/beds")
public class BedController {

    private final BedService bedService;

    @Autowired
    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    /**
     * Lists beds with search and filtering, plus live operational summary counters.
     */
    @GetMapping
    public String listBeds(@RequestParam(value = "query", required = false) String query,
                           @RequestParam(value = "ward", required = false) String ward,
                           @RequestParam(value = "type", required = false) BedType type,
                           @RequestParam(value = "status", required = false) BedStatus status,
                           Model model) {
        List<Bed> beds = bedService.searchAndFilterBeds(query, ward, type, status);
        model.addAttribute("beds", beds);
        model.addAttribute("wards", bedService.getDistinctWards());
        model.addAttribute("bedTypes", BedType.values());
        model.addAttribute("bedStatuses", BedStatus.values());

        // Live stats on top of bed view
        model.addAttribute("totalBeds", bedService.getTotalBedCount());
        model.addAttribute("availableBeds", bedService.getAvailableBedCount());
        model.addAttribute("occupiedBeds", bedService.getOccupiedBedCount());
        model.addAttribute("maintenanceBeds", bedService.getMaintenanceBedCount());

        // Retain filters in view
        model.addAttribute("query", query);
        model.addAttribute("selectedWard", ward);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "Bed Management");
        return "bed/list";
    }

    /**
     * Shows form to register a new bed.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        if (!model.containsAttribute("bed")) {
            model.addAttribute("bed", new Bed());
        }
        model.addAttribute("bedTypes", BedType.values());
        model.addAttribute("bedStatuses", BedStatus.values());
        model.addAttribute("pageTitle", "Register New Bed");
        return "bed/form";
    }

    /**
     * Handles bed creation.
     */
    @PostMapping
    public String createBed(@Valid @ModelAttribute("bed") Bed bed,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (result.hasErrors()) {
            model.addAttribute("bedTypes", BedType.values());
            model.addAttribute("bedStatuses", BedStatus.values());
            model.addAttribute("pageTitle", "Register New Bed");
            return "bed/form";
        }

        try {
            Bed saved = bedService.createBed(bed);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bed " + saved.getBedNumber() + " registered successfully in " + saved.getWard() + ".");
            return "redirect:/beds";
        } catch (IllegalArgumentException ex) {
            result.rejectValue("bedNumber", "error.bed", ex.getMessage());
            model.addAttribute("bedTypes", BedType.values());
            model.addAttribute("bedStatuses", BedStatus.values());
            model.addAttribute("pageTitle", "Register New Bed");
            return "bed/form";
        }
    }

    /**
     * Shows details of a specific bed, including current active occupant and historical stays.
     */
    @GetMapping("/{id}")
    public String viewBed(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Bed> bedOpt = bedService.getBedById(id);
        if (bedOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bed not found with ID: " + id);
            return "redirect:/beds";
        }

        Bed bed = bedOpt.get();
        Optional<Admission> activeAdmission = bedService.getActiveAdmissionForBed(id);
        List<Admission> admissionHistory = bedService.getAdmissionHistoryForBed(id);

        model.addAttribute("bed", bed);
        model.addAttribute("activeAdmission", activeAdmission.orElse(null));
        model.addAttribute("admissionHistory", admissionHistory);
        model.addAttribute("pageTitle", "Bed Details – " + bed.getBedNumber());
        return "bed/view";
    }

    /**
     * Shows edit form for an existing bed.
     */
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Bed> bedOpt = bedService.getBedById(id);
        if (bedOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bed not found with ID: " + id);
            return "redirect:/beds";
        }

        Bed bed = bedOpt.get();
        boolean hasActiveAdmission = bedService.getActiveAdmissionForBed(id).isPresent();

        model.addAttribute("bed", bed);
        model.addAttribute("hasActiveAdmission", hasActiveAdmission);
        model.addAttribute("bedTypes", BedType.values());
        model.addAttribute("bedStatuses", BedStatus.values());
        model.addAttribute("pageTitle", "Edit Bed – " + bed.getBedNumber());
        return "bed/form";
    }

    /**
     * Handles bed updates.
     */
    @PostMapping("/{id}")
    public String updateBed(@PathVariable("id") Long id,
                            @Valid @ModelAttribute("bed") Bed bed,
                            BindingResult result,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (result.hasErrors()) {
            boolean hasActiveAdmission = bedService.getActiveAdmissionForBed(id).isPresent();
            model.addAttribute("hasActiveAdmission", hasActiveAdmission);
            model.addAttribute("bedTypes", BedType.values());
            model.addAttribute("bedStatuses", BedStatus.values());
            model.addAttribute("pageTitle", "Edit Bed – " + bed.getBedNumber());
            return "bed/form";
        }

        try {
            bedService.updateBed(id, bed);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bed " + bed.getBedNumber() + " updated successfully.");
            return "redirect:/beds/" + id;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            boolean hasActiveAdmission = bedService.getActiveAdmissionForBed(id).isPresent();
            model.addAttribute("hasActiveAdmission", hasActiveAdmission);
            model.addAttribute("bedTypes", BedType.values());
            model.addAttribute("bedStatuses", BedStatus.values());
            model.addAttribute("pageTitle", "Edit Bed – " + bed.getBedNumber());
            return "bed/form";
        }
    }

    /**
     * Quick action to place an unoccupied bed under maintenance.
     */
    @PostMapping("/{id}/maintenance")
    public String setMaintenance(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Bed bed = bedService.markUnderMaintenance(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bed " + bed.getBedNumber() + " is now marked UNDER MAINTENANCE.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/beds";
    }

    /**
     * Quick action to return a maintenance bed back to AVAILABLE.
     */
    @PostMapping("/{id}/available")
    public String setAvailable(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Bed bed = bedService.returnToAvailable(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Bed " + bed.getBedNumber() + " returned to AVAILABLE status.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/beds";
    }

    /**
     * Safe deletion of a bed if no history exists.
     */
    @PostMapping("/{id}/delete")
    public String deleteBed(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            bedService.deleteBed(id);
            redirectAttributes.addFlashAttribute("successMessage", "Bed deleted successfully.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/beds";
    }
}
