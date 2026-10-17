package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedType;
import com.hospital.hospital_management.service.BedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BedService bedService;

    private Long savedBedId;
    private String savedBedNumber;

    @BeforeEach
    void setUp() {
        savedBedNumber = "TEST-B-" + System.currentTimeMillis() % 100000;
        Bed bed = new Bed(savedBedNumber, "Cardiology Ward", BedType.PRIVATE, "3rd Floor",
                new BigDecimal("1200.00"), "Equipped with cardiac monitor");
        Bed saved = bedService.createBed(bed);
        savedBedId = saved.getId();
    }

    @Test
    void testListBedsPageLoads() throws Exception {
        mockMvc.perform(get("/beds").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/list"))
                .andExpect(model().attributeExists("beds"))
                .andExpect(model().attributeExists("totalBeds"))
                .andExpect(model().attributeExists("availableBeds"))
                .andExpect(model().attributeExists("occupiedBeds"));
    }

    @Test
    void testSearchAndFilterBeds() throws Exception {
        mockMvc.perform(get("/beds").with(user("receptionist").roles("RECEPTIONIST"))
                        .param("query", savedBedNumber)
                        .param("ward", "Cardiology Ward")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/list"))
                .andExpect(model().attributeExists("beds"));
    }

    @Test
    void testShowCreateBedForm() throws Exception {
        mockMvc.perform(get("/beds/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/form"))
                .andExpect(model().attributeExists("bed"))
                .andExpect(model().attributeExists("bedTypes"))
                .andExpect(model().attributeExists("bedStatuses"));
    }

    @Test
    void testCreateBedSuccess() throws Exception {
        String newBedNumber = "NEW-" + System.currentTimeMillis() % 100000;
        mockMvc.perform(post("/beds").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("bedNumber", newBedNumber)
                        .param("ward", "General Ward X")
                        .param("bedType", "GENERAL")
                        .param("floor", "1st Floor")
                        .param("dailyCharge", "450.00")
                        .param("description", "Window bed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testCreateBedValidationFailure() throws Exception {
        mockMvc.perform(post("/beds").with(user("admin").roles("ADMIN")).with(csrf())
                        .param("bedNumber", "")
                        .param("ward", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/form"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testViewBedDetails() throws Exception {
        mockMvc.perform(get("/beds/{id}", savedBedId).with(user("doctor").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/view"))
                .andExpect(model().attributeExists("bed"))
                .andExpect(model().attributeExists("admissionHistory"));
    }

    @Test
    void testViewBedNotFound() throws Exception {
        mockMvc.perform(get("/beds/{id}", 999999L).with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testShowEditBedForm() throws Exception {
        mockMvc.perform(get("/beds/{id}/edit", savedBedId).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("bed/form"))
                .andExpect(model().attributeExists("bed"));
    }

    @Test
    void testUpdateBedSuccess() throws Exception {
        mockMvc.perform(post("/beds/{id}", savedBedId).with(user("admin").roles("ADMIN")).with(csrf())
                        .param("bedNumber", savedBedNumber)
                        .param("ward", "Updated Ward")
                        .param("bedType", "ICU")
                        .param("floor", "2nd Floor")
                        .param("dailyCharge", "1500.00")
                        .param("status", "AVAILABLE")
                        .param("description", "Updated desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds/" + savedBedId))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testSetMaintenanceAndAvailableToggle() throws Exception {
        // 1. Mark Maintenance
        mockMvc.perform(post("/beds/{id}/maintenance", savedBedId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds"))
                .andExpect(flash().attributeExists("successMessage"));

        // 2. Return to Available
        mockMvc.perform(post("/beds/{id}/available", savedBedId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testDeleteBed() throws Exception {
        String tempBedNumber = "DEL-" + System.currentTimeMillis() % 100000;
        Bed toDelete = bedService.createBed(new Bed(tempBedNumber, "Temp Ward", BedType.GENERAL, "1st",
                BigDecimal.ZERO, ""));

        mockMvc.perform(post("/beds/{id}/delete", toDelete.getId()).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/beds"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/beds"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testReceptionistCannotAccessCreateBedForm() throws Exception {
        mockMvc.perform(get("/beds/new").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBillingStaffCannotCreateBed() throws Exception {
        mockMvc.perform(post("/beds").with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("bedNumber", "FORBIDDEN-1")
                        .param("ward", "General"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testReceptionistCannotDeleteBed() throws Exception {
        mockMvc.perform(post("/beds/{id}/delete", savedBedId).with(user("receptionist").roles("RECEPTIONIST")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
