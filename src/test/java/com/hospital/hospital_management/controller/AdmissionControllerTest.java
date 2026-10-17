package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.service.AdmissionService;
import com.hospital.hospital_management.service.BedService;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AdmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdmissionService admissionService;

    @Autowired
    private BedService bedService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    private Long savedPatientId;
    private Long savedBedId;
    private Long savedDoctorId;
    private Long savedAdmissionId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis() % 100000;
        Patient patient = new Patient("Inpatient" + ts, "Kumar", 42, "Male", "B+",
                "987" + String.format("%07d", ts), "inpatient" + ts + "@example.com", "Pune", "None");
        Patient savedPatient = patientService.savePatient(patient);
        savedPatientId = savedPatient.getId();

        Doctor doctor = new Doctor("Suresh", "Patil", "Surgeon", "Surgery",
                "986" + String.format("%07d", ts), "dr.suresh" + ts + "@hospital.com", 800.0, "All Days");
        Doctor savedDoctor = doctorService.saveDoctor(doctor);
        savedDoctorId = savedDoctor.getId();

        Bed bed = new Bed("ADM-B-" + ts, "Surgical Ward", BedType.SEMI_PRIVATE, "2nd Floor",
                new BigDecimal("900.00"), "Post-op bed");
        Bed savedBed = bedService.createBed(bed);
        savedBedId = savedBed.getId();

        Admission admission = admissionService.admitPatient(savedPatientId, savedBedId, savedDoctorId,
                LocalDateTime.now().minusHours(5), "Pre-surgery prep");
        savedAdmissionId = admission.getId();
    }

    @Test
    void testListAdmissionsPageLoads() throws Exception {
        mockMvc.perform(get("/admissions").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/list"))
                .andExpect(model().attributeExists("admissions"))
                .andExpect(model().attributeExists("activeAdmissionsCount"))
                .andExpect(model().attributeExists("todayAdmissionsCount"));
    }

    @Test
    void testSearchAndFilterAdmissions() throws Exception {
        mockMvc.perform(get("/admissions").with(user("receptionist").roles("RECEPTIONIST"))
                        .param("status", "ACTIVE")
                        .param("ward", "Surgical Ward"))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/list"))
                .andExpect(model().attributeExists("admissions"));
    }

    @Test
    void testShowAdmissionForm() throws Exception {
        mockMvc.perform(get("/admissions/new").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/form"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("availableBeds"))
                .andExpect(model().attributeExists("doctors"));
    }

    @Test
    void testAdmitPatientSuccess() throws Exception {
        long ts = (System.currentTimeMillis() + 100) % 100000;
        Patient newPatient = patientService.savePatient(new Patient("P" + ts, "L" + ts, 28, "Female", "A+",
                "981" + String.format("%07d", ts), "p" + ts + "@test.com", "City", "None"));
        Bed newBed = bedService.createBed(new Bed("FREE-" + ts, "General Ward", BedType.GENERAL, "1st",
                new BigDecimal("400.00"), ""));

        mockMvc.perform(post("/admissions").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                        .param("patientId", newPatient.getId().toString())
                        .param("bedId", newBed.getId().toString())
                        .param("doctorId", savedDoctorId.toString())
                        .param("admissionDate", LocalDateTime.now().toString())
                        .param("admissionReason", "Fever monitoring"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testAdmitPatientMissingFieldsRejection() throws Exception {
        mockMvc.perform(post("/admissions").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                        .param("admissionReason", "Incomplete submission"))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testViewAdmissionDetails() throws Exception {
        mockMvc.perform(get("/admissions/{id}", savedAdmissionId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/view"))
                .andExpect(model().attributeExists("admission"));
    }

    @Test
    void testViewAdmissionNotFound() throws Exception {
        mockMvc.perform(get("/admissions/{id}", 999999L).with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admissions"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testShowDischargeForm() throws Exception {
        mockMvc.perform(get("/admissions/{id}/discharge", savedAdmissionId).with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/discharge"))
                .andExpect(model().attributeExists("admission"))
                .andExpect(model().attributeExists("defaultDischargeDate"));
    }

    @Test
    void testDischargePatientSuccess() throws Exception {
        mockMvc.perform(post("/admissions/{id}/discharge", savedAdmissionId).with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                        .param("dischargeDate", LocalDateTime.now().toString())
                        .param("dischargeNotes", "Successfully discharged with full recovery."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admissions/" + savedAdmissionId))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testDischargeAlreadyDischarged() throws Exception {
        // Discharge once
        admissionService.dischargePatient(savedAdmissionId, LocalDateTime.now(), "Discharged");

        // Attempt discharging again
        mockMvc.perform(post("/admissions/{id}/discharge", savedAdmissionId).with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                        .param("dischargeDate", LocalDateTime.now().toString())
                        .param("dischargeNotes", "Duplicate discharge attempt"))
                .andExpect(status().isOk())
                .andExpect(view().name("admission/discharge"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admissions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testBillingStaffCannotAccessAdmissionForm() throws Exception {
        mockMvc.perform(get("/admissions/new").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBillingStaffCannotAdmitPatient() throws Exception {
        mockMvc.perform(post("/admissions").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("admissionReason", "Attempt"))
                .andExpect(status().isForbidden());
    }
}
