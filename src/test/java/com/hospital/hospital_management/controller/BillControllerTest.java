package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private BedService bedService;

    @Autowired
    private AdmissionService admissionService;

    private Long savedPatientId;
    private Long savedAppointmentId;
    private Long savedAdmissionId;
    private Long savedBillId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis() % 100000;
        Patient patient = patientService.savePatient(new Patient("BillPatient" + ts, "Test", 35, "Female", "O+",
                "982" + String.format("%07d", ts), "bp" + ts + "@hospital.com", "Mumbai", "None"));
        savedPatientId = patient.getId();

        Doctor doctor = doctorService.saveDoctor(new Doctor("Doc" + ts, "Sharma", "General Medicine", "OPD",
                "983" + String.format("%07d", ts), "dr" + ts + "@hospital.com", 600.0, "Mon-Sat"));

        Appointment appointment = appointmentService.bookAppointment(new Appointment(patient, doctor,
                LocalDate.now().plusDays(2), LocalTime.of(14, 0), LocalTime.of(14, 30), "Fever checkup"));
        savedAppointmentId = appointment.getId();

        Bed bed = bedService.createBed(new Bed("BILL-B-" + ts, "General Ward", BedType.GENERAL, "1st",
                new BigDecimal("500.00"), ""));

        Admission admission = admissionService.admitPatient(savedPatientId, bed.getId(), doctor.getId(),
                LocalDateTime.now().minusDays(1), "Observation stay");
        savedAdmissionId = admission.getId();

        Bill bill = billingService.createManualBill(savedPatientId,
                Collections.singletonList(new BillItem("General Medicine", 1, new BigDecimal("350.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, "Sample bill");
        savedBillId = bill.getId();
    }

    @Test
    void testListBillsPageLoads() throws Exception {
        mockMvc.perform(get("/bills").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/list"))
                .andExpect(model().attributeExists("bills"))
                .andExpect(model().attributeExists("totalBills"))
                .andExpect(model().attributeExists("totalBilledAmount"))
                .andExpect(model().attributeExists("totalCollectedAmount"))
                .andExpect(model().attributeExists("totalOutstandingBalance"));
    }

    @Test
    void testFilterBillsByStatus() throws Exception {
        mockMvc.perform(get("/bills").with(user("billing").roles("BILLING_STAFF")).param("status", "UNPAID"))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/list"))
                .andExpect(model().attributeExists("bills"));
    }

    @Test
    void testShowCreateBillForm() throws Exception {
        mockMvc.perform(get("/bills/new").with(user("billing").roles("BILLING_STAFF")).param("patientId", savedPatientId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/form"))
                .andExpect(model().attributeExists("patients"));
    }

    @Test
    void testCreateManualBillSuccess() throws Exception {
        mockMvc.perform(post("/bills").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("patientId", savedPatientId.toString())
                        .param("itemDescriptions", "Diagnostic Lab Tests", "Prescription Drugs")
                        .param("itemQuantities", "1", "2")
                        .param("itemUnitPrices", "500.00", "150.00")
                        .param("discountAmount", "50.00")
                        .param("taxAmount", "25.00")
                        .param("notes", "Outpatient bill"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateManualBillValidationFailure() throws Exception {
        mockMvc.perform(post("/bills").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("itemDescriptions", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testGenerateFromAppointmentSuccess() throws Exception {
        mockMvc.perform(post("/bills/from-appointment").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("appointmentId", savedAppointmentId.toString())
                        .param("discountAmount", "0.00")
                        .param("taxAmount", "0.00")
                        .param("notes", "Fast generation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testGenerateFromAdmissionSuccess() throws Exception {
        mockMvc.perform(post("/bills/from-admission").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("admissionId", savedAdmissionId.toString())
                        .param("discountAmount", "100.00")
                        .param("taxAmount", "0.00")
                        .param("notes", "Interim inpatient invoice"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testViewBillDetails() throws Exception {
        mockMvc.perform(get("/bills/{id}", savedBillId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/view"))
                .andExpect(model().attributeExists("bill"))
                .andExpect(model().attributeExists("payments"));
    }

    @Test
    void testViewBillNotFound() throws Exception {
        mockMvc.perform(get("/bills/{id}", 999999L).with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bills"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testPrintBillReceipt() throws Exception {
        mockMvc.perform(get("/bills/{id}/print", savedBillId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/print"))
                .andExpect(model().attributeExists("bill"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/bills"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testReceptionistCannotAccessCreateBillForm() throws Exception {
        mockMvc.perform(get("/bills/new").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoctorCannotCreateBill() throws Exception {
        mockMvc.perform(post("/bills").with(user("doctor").roles("DOCTOR")).with(csrf())
                        .param("patientId", savedPatientId.toString()))
                .andExpect(status().isForbidden());
    }
}
