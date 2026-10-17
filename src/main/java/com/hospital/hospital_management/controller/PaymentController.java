package com.hospital.hospital_management.controller;

import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;
import com.hospital.hospital_management.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class PaymentController {

    private final BillingService billingService;

    @Autowired
    public PaymentController(BillingService billingService) {
        this.billingService = billingService;
    }

    /**
     * Lists all recorded payments in reverse chronological order.
     */
    @GetMapping("/payments")
    public String listPayments(Model model) {
        List<Payment> payments = billingService.getAllPayments();
        model.addAttribute("payments", payments);
        model.addAttribute("totalCollectedAmount", billingService.getTotalCollectedAmount());
        model.addAttribute("pageTitle", "Payment Transactions");
        return "billing/payment-list";
    }

    /**
     * Shows payment entry form for a specific bill.
     */
    @GetMapping("/bills/{id}/pay")
    public String showPaymentForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Bill> billOpt = billingService.getBillById(id);
        if (billOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice not found with ID: " + id);
            return "redirect:/bills";
        }

        Bill bill = billOpt.get();
        if (bill.getBalanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invoice " + bill.getBillNumber() + " is already fully paid.");
            return "redirect:/bills/" + id;
        }

        model.addAttribute("bill", bill);
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("defaultAmount", bill.getBalanceAmount());
        model.addAttribute("pageTitle", "Record Payment – " + bill.getBillNumber());
        return "billing/payment-form";
    }

    /**
     * Records a payment against an invoice.
     */
    @PostMapping("/bills/{id}/pay")
    public String processPayment(@PathVariable("id") Long id,
                                 @RequestParam("amount") BigDecimal amount,
                                 @RequestParam("paymentMethod") PaymentMethod paymentMethod,
                                 @RequestParam(value = "transactionReference", required = false) String transactionReference,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 RedirectAttributes redirectAttributes) {
        try {
            Payment payment = billingService.recordPayment(id, amount, paymentMethod, transactionReference, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payment of ₹" + payment.getAmount() + " recorded successfully (Ref: " + payment.getPaymentReference() + ").");
            return "redirect:/bills/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/bills/" + id;
        }
    }

    /**
     * Views an individual payment transaction receipt.
     */
    @GetMapping("/payments/{id}")
    public String viewPayment(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Payment> paymentOpt = billingService.getPaymentById(id);
        if (paymentOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Payment not found with ID: " + id);
            return "redirect:/payments";
        }

        model.addAttribute("payment", paymentOpt.get());
        model.addAttribute("pageTitle", "Payment " + paymentOpt.get().getPaymentReference());
        return "billing/payment-view";
    }
}
