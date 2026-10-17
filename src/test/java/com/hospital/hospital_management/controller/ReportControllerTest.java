package com.hospital.hospital_management.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testReportsRootRedirectsToAppointments() throws Exception {
        mockMvc.perform(get("/reports").with(user("staff").roles("RECEPTIONIST")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reports/appointments"));
    }

    @Test
    void testAppointmentReportLoadsForStaff() throws Exception {
        mockMvc.perform(get("/reports/appointments").with(user("doctor_user").roles("DOCTOR")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/appointments"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("doctors"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    void testPatientReportLoadsForStaff() throws Exception {
        mockMvc.perform(get("/reports/patients").with(user("receptionist_user").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/patients"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void testBedOccupancyReportLoadsForStaff() throws Exception {
        mockMvc.perform(get("/reports/beds").with(user("nurse").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/beds"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void testFinancialReportAccessibleByAdmin() throws Exception {
        mockMvc.perform(get("/reports/financial").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/financial"))
                .andExpect(model().attributeExists("report"))
                .andExpect(model().attributeExists("paymentMethods"));
    }

    @Test
    void testFinancialReportAccessibleByBillingStaff() throws Exception {
        mockMvc.perform(get("/reports/financial").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/financial"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void testFinancialReportForbiddenForReceptionist() throws Exception {
        mockMvc.perform(get("/reports/financial").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testFinancialReportForbiddenForDoctor() throws Exception {
        mockMvc.perform(get("/reports/financial").with(user("doc").roles("DOCTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reports/appointments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testInvalidDateFilterShowsErrorMessage() throws Exception {
        mockMvc.perform(get("/reports/appointments")
                        .param("startDate", "2026-12-31")
                        .param("endDate", "2026-01-01")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/appointments"))
                .andExpect(model().attribute("errorMessage", containsString("Start date cannot be after end date")));
    }

    @Test
    void testAppointmentReportWithFilters() throws Exception {
        mockMvc.perform(get("/reports/appointments")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .param("status", "SCHEDULED")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/appointments"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void testPatientReportWithFilters() throws Exception {
        mockMvc.perform(get("/reports/patients")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .param("gender", "Female")
                        .param("bloodGroup", "O+")
                        .with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/patients"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    void testFinancialReportWithPaymentMethodFilter() throws Exception {
        mockMvc.perform(get("/reports/financial")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31")
                        .param("paymentMethod", "UPI")
                        .with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("reports/financial"))
                .andExpect(model().attributeExists("report"));
    }
}
