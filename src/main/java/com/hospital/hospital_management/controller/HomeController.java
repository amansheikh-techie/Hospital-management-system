package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.service.AdmissionService;
import com.hospital.hospital_management.service.AppointmentService;
import com.hospital.hospital_management.service.BedService;
import com.hospital.hospital_management.service.DoctorService;
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
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final BedService bedService;
    private final AdmissionService admissionService;

    @Autowired
    public HomeController(PatientService patientService,
                          DoctorService doctorService,
                          AppointmentService appointmentService,
                          BedService bedService,
                          AdmissionService admissionService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.bedService = bedService;
        this.admissionService = admissionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("totalPatients", patientService.getTotalPatientCount());
        model.addAttribute("totalDoctors", doctorService.getTotalDoctorCount());
        model.addAttribute("todayAppointments", appointmentService.getTodayAppointmentCount());
        model.addAttribute("availableBeds", bedService.getAvailableBedCount());
        model.addAttribute("occupiedBeds", bedService.getOccupiedBedCount());
        model.addAttribute("totalBeds", bedService.getTotalBedCount());
        model.addAttribute("maintenanceBeds", bedService.getMaintenanceBedCount());
        model.addAttribute("activeAdmissions", admissionService.getActiveAdmissionCount());
        model.addAttribute("todayAdmissions", admissionService.getTodayAdmissionCount());
        model.addAttribute("pendingBills", 0);
        return "index";
    }
}
