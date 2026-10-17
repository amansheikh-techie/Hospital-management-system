package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.model.Role;
import com.hospital.hospital_management.model.User;
import com.hospital.hospital_management.service.AppointmentService;
import com.hospital.hospital_management.service.DoctorService;
import com.hospital.hospital_management.service.PatientService;
import com.hospital.hospital_management.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserService userService;

    private Long savedPatientId;
    private Long savedDoctorId;
    private Long savedAppointmentId;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient("Karan", "Singhania", 32, "Male", "AB+",
                "9871122334", "karan@example.com", "Civil Lines, Delhi", "None");
        Patient savedPatient = patientService.savePatient(patient);
        savedPatientId = savedPatient.getId();

        Doctor doctor = new Doctor("Meera", "Sen", "Dermatologist", "Dermatology",
                "9876599887", "dr.meera@hospital.com", 650.0, "Mon, Wed, Fri");
        Doctor savedDoctor = doctorService.saveDoctor(doctor);
        savedDoctorId = savedDoctor.getId();

        testDate = LocalDate.now().plusDays(2);

        Appointment appointment = new Appointment(savedPatient, savedDoctor, testDate,
                LocalTime.of(11, 0), LocalTime.of(11, 30), "Skin allergy consultation");
        Appointment savedAppointment = appointmentService.bookAppointment(appointment);
        savedAppointmentId = savedAppointment.getId();
    }

    @Test
    void testListAppointmentsPageLoads() throws Exception {
        mockMvc.perform(get("/appointments").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/list"))
                .andExpect(model().attributeExists("appointments"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("totalCount"))
                .andExpect(model().attributeExists("todayCount"));
    }

    @Test
    void testFilterAppointmentsByDoctorAndStatus() throws Exception {
        mockMvc.perform(get("/appointments").with(user("receptionist").roles("RECEPTIONIST"))
                .param("doctorId", savedDoctorId.toString())
                .param("status", "SCHEDULED"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/list"))
                .andExpect(model().attributeExists("appointments"))
                .andExpect(model().attribute("isFiltered", true));
    }

    @Test
    void testShowBookingForm() throws Exception {
        mockMvc.perform(get("/appointments/new").with(user("receptionist").roles("RECEPTIONIST"))
                .param("patientId", savedPatientId.toString())
                .param("doctorId", savedDoctorId.toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/form"))
                .andExpect(model().attributeExists("appointment"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attribute("pageTitle", "Book New Appointment"));
    }

    @Test
    void testBookAppointmentSuccessRedirects() throws Exception {
        // Book non-overlapping slot: 12:00 to 12:30
        mockMvc.perform(post("/appointments").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("patient.id", savedPatientId.toString())
                .param("doctor.id", savedDoctorId.toString())
                .param("appointmentDate", testDate.toString())
                .param("startTime", "12:00")
                .param("endTime", "12:30")
                .param("reason", "Follow up discussion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testBookAppointmentConflictReturnsFormWithError() throws Exception {
        // Overlap with existing appointment (11:00 to 11:30) by trying 11:15 to 11:45
        mockMvc.perform(post("/appointments").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("patient.id", savedPatientId.toString())
                .param("doctor.id", savedDoctorId.toString())
                .param("appointmentDate", testDate.toString())
                .param("startTime", "11:15")
                .param("endTime", "11:45")
                .param("reason", "Double booking attempt"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/form"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    void testBookAppointmentValidationFailureReturnsForm() throws Exception {
        // Missing reason and invalid doctor ID
        mockMvc.perform(post("/appointments").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("patient.id", savedPatientId.toString())
                .param("doctor.id", "")
                .param("appointmentDate", testDate.toString())
                .param("startTime", "10:00")
                .param("endTime", "10:30")
                .param("reason", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testViewAppointmentDetails() throws Exception {
        mockMvc.perform(get("/appointments/" + savedAppointmentId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/view"))
                .andExpect(model().attributeExists("appointment"));
    }

    @Test
    void testShowEditForm() throws Exception {
        mockMvc.perform(get("/appointments/" + savedAppointmentId + "/edit").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/form"))
                .andExpect(model().attributeExists("appointment"));
    }

    @Test
    void testUpdateAppointmentSuccessRedirects() throws Exception {
        // Reschedule to 14:00 - 14:30
        mockMvc.perform(post("/appointments/" + savedAppointmentId + "/edit").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("patient.id", savedPatientId.toString())
                .param("doctor.id", savedDoctorId.toString())
                .param("appointmentDate", testDate.toString())
                .param("startTime", "14:00")
                .param("endTime", "14:30")
                .param("reason", "Rescheduled routine check")
                .param("status", "SCHEDULED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCancelAppointmentRedirects() throws Exception {
        mockMvc.perform(post("/appointments/" + savedAppointmentId + "/cancel").with(user("receptionist").roles("RECEPTIONIST")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCompleteAppointmentRedirects() throws Exception {
        mockMvc.perform(post("/appointments/" + savedAppointmentId + "/complete").with(user("doctor").roles("DOCTOR")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/appointments"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testBillingStaffCannotAccessAppointmentBookingForm() throws Exception {
        mockMvc.perform(get("/appointments/new").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBillingStaffCannotBookAppointment() throws Exception {
        mockMvc.perform(post("/appointments").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                .param("patient.id", savedPatientId.toString())
                .param("doctor.id", savedDoctorId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnlinkedDoctorAccountShowsSafeMessageAndEmptyList() throws Exception {
        // Register doctor user without linked doctor entity
        User unlinkedDoctorUser = new User();
        unlinkedDoctorUser.setUsername("unlinked_doc");
        unlinkedDoctorUser.setFullName("Dr. Unlinked");
        unlinkedDoctorUser.setEmail("unlinked@hospital.local");
        unlinkedDoctorUser.setRole(Role.ROLE_DOCTOR);
        unlinkedDoctorUser.setDoctor(null);
        userService.createUser(unlinkedDoctorUser, "Password123!");

        mockMvc.perform(get("/appointments").with(user("unlinked_doc").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment/list"))
                .andExpect(model().attributeExists("warningMessage"))
                .andExpect(model().attribute("appointments", java.util.Collections.emptyList()));
    }
}
