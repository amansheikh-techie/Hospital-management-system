package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.BillItem;
import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;
import com.hospital.hospital_management.service.BillingService;
import com.hospital.hospital_management.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillingService billingService;

    @Autowired
    private PatientService patientService;

    private Long savedBillId;
    private Long savedPaymentId;

    @BeforeEach
    void setUp() {
        long ts = System.currentTimeMillis() % 100000;
        Patient patient = patientService.savePatient(new Patient("PayPatient" + ts, "Test", 40, "Male", "AB+",
                "984" + String.format("%07d", ts), "pp" + ts + "@hospital.com", "Nagpur", "None"));

        Bill bill = billingService.createManualBill(patient.getId(),
                Collections.singletonList(new BillItem("Cardiology Checkup", 1, new BigDecimal("1000.00"))),
                BigDecimal.ZERO, BigDecimal.ZERO, "Pay test bill");
        savedBillId = bill.getId();

        Payment payment = billingService.recordPayment(savedBillId, new BigDecimal("300.00"),
                PaymentMethod.UPI, "UPI-TEST-" + ts, "Initial partial payment");
        savedPaymentId = payment.getId();
    }

    @Test
    void testListPaymentsPageLoads() throws Exception {
        mockMvc.perform(get("/payments").with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/payment-list"))
                .andExpect(model().attributeExists("payments"))
                .andExpect(model().attributeExists("totalCollectedAmount"));
    }

    @Test
    void testShowPaymentForm() throws Exception {
        mockMvc.perform(get("/bills/{id}/pay", savedBillId).with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/payment-form"))
                .andExpect(model().attributeExists("bill"))
                .andExpect(model().attributeExists("defaultAmount"));
    }

    @Test
    void testProcessPaymentSuccess() throws Exception {
        mockMvc.perform(post("/bills/{id}/pay", savedBillId).with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("amount", "500.00")
                        .param("paymentMethod", "CARD")
                        .param("transactionReference", "CARD-" + System.currentTimeMillis())
                        .param("notes", "Second partial payment"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bills/" + savedBillId))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testProcessPaymentExceedsBalanceRejection() throws Exception {
        mockMvc.perform(post("/bills/{id}/pay", savedBillId).with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("amount", "9999.00")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bills/" + savedBillId))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testProcessPaymentDuplicateTransactionRefRejection() throws Exception {
        String existingRef = "UPI-EXISTING-" + System.currentTimeMillis();
        billingService.recordPayment(savedBillId, new BigDecimal("100.00"),
                PaymentMethod.UPI, existingRef, "First payment with ref");

        // Attempt second payment with the same transaction reference
        mockMvc.perform(post("/bills/{id}/pay", savedBillId).with(user("billing").roles("BILLING_STAFF")).with(csrf())
                        .param("amount", "50.00")
                        .param("paymentMethod", "UPI")
                        .param("transactionReference", existingRef))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/bills/" + savedBillId))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testViewPaymentDetails() throws Exception {
        mockMvc.perform(get("/payments/{id}", savedPaymentId).with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().isOk())
                .andExpect(view().name("billing/payment-view"))
                .andExpect(model().attributeExists("payment"));
    }

    @Test
    void testViewPaymentNotFound() throws Exception {
        mockMvc.perform(get("/payments/{id}", 999999L).with(user("billing").roles("BILLING_STAFF")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // --- Negative Authorization Tests ---

    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/payments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testReceptionistCannotAccessPayments() throws Exception {
        mockMvc.perform(get("/payments").with(user("receptionist").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDoctorCannotProcessPayment() throws Exception {
        mockMvc.perform(post("/bills/{id}/pay", savedBillId).with(user("doctor").roles("DOCTOR")).with(csrf())
                        .param("amount", "100.00")
                        .param("paymentMethod", "CASH"))
                .andExpect(status().isForbidden());
    }
}
