package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController serves the main dashboard page.
 * Displays live stats from services as they are built.
 */
@Controller
public class HomeController {

    private final PatientService patientService;

    @Autowired
    public HomeController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalPatients", patientService.getTotalPatientCount());
        model.addAttribute("totalDoctors", 0);
        model.addAttribute("todayAppointments", 0);
        model.addAttribute("availableBeds", 0);
        model.addAttribute("occupiedBeds", 0);
        model.addAttribute("pendingBills", 0);
        return "index";
    }
}
