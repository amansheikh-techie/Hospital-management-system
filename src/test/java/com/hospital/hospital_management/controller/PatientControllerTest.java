package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientService patientService;

    private Long savedPatientId;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient(
                "Priya", "Patel", 24, "Female", "A+",
                "9876501234", "priya@example.com",
                "78 Ring Road, Ahmedabad", "No allergies"
        );
        Patient saved = patientService.savePatient(patient);
        savedPatientId = saved.getId();
    }

    @Test
    void testListPatientsPageLoads() throws Exception {
        mockMvc.perform(get("/patients").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("totalCount"));
    }

    @Test
    void testSearchPatientsWithKeyword() throws Exception {
        mockMvc.perform(get("/patients").with(user("receptionist").roles("RECEPTIONIST")).param("keyword", "Priya"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attribute("keyword", "Priya"));
    }

    @Test
    void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/patients/new").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/form"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attribute("pageTitle", "Register New Patient"));
    }

    @Test
    void testRegisterPatientSuccessRedirects() throws Exception {
        mockMvc.perform(post("/patients").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("firstName", "Suresh")
                .param("lastName", "Kumar")
                .param("age", "35")
                .param("gender", "Male")
                .param("bloodGroup", "O+")
                .param("contactNumber", "9811223344")
                .param("email", "suresh@example.com")
                .param("address", "Sector 14, Gurgaon")
                .param("medicalHistory", "Hypertension"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testRegisterPatientValidationFailureReturnsForm() throws Exception {
        // Missing required fields (firstName, lastName, phone format invalid)
        mockMvc.perform(post("/patients").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("firstName", "")
                .param("lastName", "")
                .param("age", "200")
                .param("contactNumber", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testViewPatientDetails() throws Exception {
        mockMvc.perform(get("/patients/" + savedPatientId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/view"))
                .andExpect(model().attributeExists("patient"));
    }

    @Test
    void testViewPatientNotFoundRedirects() throws Exception {
        mockMvc.perform(get("/patients/99999").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testShowEditForm() throws Exception {
        mockMvc.perform(get("/patients/" + savedPatientId + "/edit").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/form"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attributeExists("pageTitle"));
    }

    @Test
    void testUpdatePatientSuccessRedirects() throws Exception {
        mockMvc.perform(post("/patients/" + savedPatientId + "/edit").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("firstName", "Priya")
                .param("lastName", "Shah")
                .param("age", "25")
                .param("gender", "Female")
                .param("bloodGroup", "A+")
                .param("contactNumber", "9876501234")
                .param("email", "priya.shah@example.com")
                .param("address", "Updated Address, Ahmedabad")
                .param("medicalHistory", "Allergic to dust"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testDeletePatientRedirects() throws Exception {
        mockMvc.perform(post("/patients/" + savedPatientId + "/delete").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/patients"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testBillingStaffCannotAccessPatientRegistrationForm() throws Exception {
        mockMvc.perform(get("/patients/new").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBillingStaffCannotRegisterPatient() throws Exception {
        mockMvc.perform(post("/patients").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                .param("firstName", "Unauthorized")
                .param("lastName", "Attempt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReceptionistCannotDeletePatient() throws Exception {
        mockMvc.perform(post("/patients/" + savedPatientId + "/delete").with(user("receptionist").roles("RECEPTIONIST")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
