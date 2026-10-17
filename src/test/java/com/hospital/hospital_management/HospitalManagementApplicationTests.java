package com.hospital.hospital_management;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

/**
 * Basic integration tests for the Hospital Management System.
 *
 * Uses an in-memory H2 database (see src/test/resources/application.properties)
 * so these tests run without a MySQL server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class HospitalManagementApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
		// Verifies that the Spring application context starts
	}

	@Test
	void dashboardPageLoads() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attributeExists("totalPatients"))
				.andExpect(model().attributeExists("totalDoctors"))
				.andExpect(model().attributeExists("todayAppointments"))
				.andExpect(model().attributeExists("availableBeds"))
				.andExpect(model().attributeExists("occupiedBeds"))
				.andExpect(model().attributeExists("totalBeds"))
				.andExpect(model().attributeExists("activeAdmissions"))
				.andExpect(model().attributeExists("pendingBills"));
	}
}
