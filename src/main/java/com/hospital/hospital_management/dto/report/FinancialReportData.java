package com.hospital.hospital_management.dto.report;

import com.hospital.hospital_management.model.Bill;
import com.hospital.hospital_management.model.Payment;
import com.hospital.hospital_management.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinancialReportData {
    private LocalDate startDate;
    private LocalDate endDate;
    private PaymentMethod paymentMethod;

    private List<Payment> payments = new ArrayList<>();
    private List<Bill> bills = new ArrayList<>();

    private BigDecimal totalBilled = BigDecimal.ZERO;
    private BigDecimal totalCollected = BigDecimal.ZERO;
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    private long totalPaymentsCount;
    private long totalBillsCount;

    private Map<PaymentMethod, BigDecimal> paymentMethodAmountBreakdown = new LinkedHashMap<>();
    private Map<PaymentMethod, Long> paymentMethodCountBreakdown = new LinkedHashMap<>();

    public FinancialReportData() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> payments) { this.payments = payments; }

    public List<Bill> getBills() { return bills; }
    public void setBills(List<Bill> bills) { this.bills = bills; }

    public BigDecimal getTotalBilled() { return totalBilled; }
    public void setTotalBilled(BigDecimal totalBilled) { this.totalBilled = totalBilled; }

    public BigDecimal getTotalCollected() { return totalCollected; }
    public void setTotalCollected(BigDecimal totalCollected) { this.totalCollected = totalCollected; }

    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public double getCollectionRatio() {
        if (totalBilled == null || totalBilled.compareTo(BigDecimal.ZERO) <= 0) {
            return 100.0;
        }
        return totalCollected.multiply(BigDecimal.valueOf(100))
                .divide(totalBilled, 1, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

    public long getTotalPaymentsCount() { return totalPaymentsCount; }

    public void setTotalPaymentsCount(long totalPaymentsCount) { this.totalPaymentsCount = totalPaymentsCount; }

    public long getTotalBillsCount() { return totalBillsCount; }
    public void setTotalBillsCount(long totalBillsCount) { this.totalBillsCount = totalBillsCount; }

    public Map<PaymentMethod, BigDecimal> getPaymentMethodAmountBreakdown() { return paymentMethodAmountBreakdown; }
    public void setPaymentMethodAmountBreakdown(Map<PaymentMethod, BigDecimal> paymentMethodAmountBreakdown) {
        this.paymentMethodAmountBreakdown = paymentMethodAmountBreakdown;
    }

    public Map<PaymentMethod, Long> getPaymentMethodCountBreakdown() { return paymentMethodCountBreakdown; }
    public void setPaymentMethodCountBreakdown(Map<PaymentMethod, Long> paymentMethodCountBreakdown) {
        this.paymentMethodCountBreakdown = paymentMethodCountBreakdown;
    }
}
