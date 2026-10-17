package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.service.DoctorService;
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
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorService doctorService;

    private Long savedDoctorId;

    @BeforeEach
    void setUp() {
        Doctor doctor = new Doctor(
                "Vikram", "Mehta", "Neurologist", "Neurology",
                "9822334455", "dr.vikram@hospital.com", 750.0, "Tue, Thu, Sat"
        );
        Doctor saved = doctorService.saveDoctor(doctor);
        savedDoctorId = saved.getId();
    }

    @Test
    void testListDoctorsPageLoads() throws Exception {
        mockMvc.perform(get("/doctors").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/list"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attributeExists("totalCount"));
    }

    @Test
    void testReceptionistCanViewDoctorsList() throws Exception {
        mockMvc.perform(get("/doctors").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/list"))
                .andExpect(model().attributeExists("doctors"));
    }

    @Test
    void testSearchDoctorsWithKeyword() throws Exception {
        mockMvc.perform(get("/doctors").with(user("admin").roles("ADMIN")).param("keyword", "Neurology"))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/list"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attribute("keyword", "Neurology"));
    }

    @Test
    void testShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/doctors/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/form"))
                .andExpect(model().attributeExists("doctor"))
                .andExpect(model().attribute("pageTitle", "Add New Doctor"));
    }

    @Test
    void testRegisterDoctorSuccessRedirects() throws Exception {
        mockMvc.perform(post("/doctors").with(user("admin").roles("ADMIN")).with(csrf())
                .param("firstName", "Sneha")
                .param("lastName", "Rao")
                .param("specialization", "Pediatrician")
                .param("department", "Pediatrics")
                .param("contactNumber", "9844556677")
                .param("email", "dr.sneha@hospital.com")
                .param("consultationFee", "500.00")
                .param("availableDays", "Mon - Fri (9 AM - 1 PM)"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctors"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testRegisterDoctorValidationFailureReturnsForm() throws Exception {
        // Missing required fields & invalid consultation fee
        mockMvc.perform(post("/doctors").with(user("admin").roles("ADMIN")).with(csrf())
                .param("firstName", "")
                .param("lastName", "")
                .param("contactNumber", "123")
                .param("consultationFee", "-50"))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testViewDoctorDetails() throws Exception {
        mockMvc.perform(get("/doctors/" + savedDoctorId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/view"))
                .andExpect(model().attributeExists("doctor"));
    }

    @Test
    void testViewDoctorNotFoundRedirects() throws Exception {
        mockMvc.perform(get("/doctors/99999").with(user("admin").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctors"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testShowEditForm() throws Exception {
        mockMvc.perform(get("/doctors/" + savedDoctorId + "/edit").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("doctor/form"))
                .andExpect(model().attributeExists("doctor"))
                .andExpect(model().attributeExists("pageTitle"));
    }

    @Test
    void testUpdateDoctorSuccessRedirects() throws Exception {
        mockMvc.perform(post("/doctors/" + savedDoctorId + "/edit").with(user("admin").roles("ADMIN")).with(csrf())
                .param("firstName", "Vikram")
                .param("lastName", "Mehta")
                .param("specialization", "Senior Neurologist")
                .param("department", "Neurology")
                .param("contactNumber", "9822334455")
                .param("email", "dr.vikram@hospital.com")
                .param("consultationFee", "900.00")
                .param("availableDays", "Mon - Sat"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctors"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testDeleteDoctorRedirects() throws Exception {
        mockMvc.perform(post("/doctors/" + savedDoctorId + "/delete").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/doctors"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/doctors"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testReceptionistCannotAccessDoctorRegistrationForm() throws Exception {
        mockMvc.perform(get("/doctors/new").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReceptionistCannotRegisterDoctor() throws Exception {
        mockMvc.perform(post("/doctors").with(user("receptionist").roles("RECEPTIONIST")).with(csrf())
                .param("firstName", "Sneha")
                .param("lastName", "Rao"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoctorCannotDeleteDoctor() throws Exception {
        mockMvc.perform(post("/doctors/" + savedDoctorId + "/delete").with(user("doctor").roles("DOCTOR")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
