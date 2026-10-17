package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

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
        mockMvc.perform(get("/patients"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attributeExists("totalCount"));
    }

    @Test
    void testSearchPatientsWithKeyword() throws Exception {
        mockMvc.perform(get("/patients").param("keyword", "Priya"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/list"))
                .andExpect(model().attributeExists("patients"))
                .andExpect(model().attribute("keyword", "Priya"));
    }

    @Test
    void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/patients/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/form"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attribute("pageTitle", "Register New Patient"));
    }

    @Test
    void testRegisterPatientSuccessRedirects() throws Exception {
        mockMvc.perform(post("/patients")
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
        mockMvc.perform(post("/patients")
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
        mockMvc.perform(get("/patients/" + savedPatientId))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/view"))
                .andExpect(model().attributeExists("patient"));
    }

    @Test
    void testViewPatientNotFoundRedirects() throws Exception {
        mockMvc.perform(get("/patients/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testShowEditForm() throws Exception {
        mockMvc.perform(get("/patients/" + savedPatientId + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("patient/form"))
                .andExpect(model().attributeExists("patient"))
                .andExpect(model().attributeExists("pageTitle"));
    }

    @Test
    void testUpdatePatientSuccessRedirects() throws Exception {
        mockMvc.perform(post("/patients/" + savedPatientId + "/edit")
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
        mockMvc.perform(post("/patients/" + savedPatientId + "/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/patients"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}
