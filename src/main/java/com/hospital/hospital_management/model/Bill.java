package com.hospital.hospital_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an invoice/bill issued to a patient.
 *
 * Mapped to the 'bills' table in MySQL hospital_db.
 */
@Entity
@Table(name = "bills", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bill_number", columnNames = "bill_number"),
        @UniqueConstraint(name = "uk_bill_appointment", columnNames = "appointment_id"),
        @UniqueConstraint(name = "uk_bill_admission", columnNames = "admission_id")
})
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Invoice number is required")
    @Size(max = 30)
    @Column(name = "bill_number", nullable = false, unique = true, length = 30)
    private String billNumber;

    @NotNull(message = "Please select a patient")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "admission_id")
    private Admission admission;

    @NotNull(message = "Invoice date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "bill_date", nullable = false)
    private LocalDateTime billDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillStatus status;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    @Column(name = "balance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    @Column(name = "is_preliminary", nullable = false)
    private boolean isPreliminary;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BillItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("paymentDate DESC")
    private List<Payment> payments = new ArrayList<>();

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Bill() {
        this.billDate = LocalDateTime.now();
        this.status = BillStatus.UNPAID;
        this.subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.balanceAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.createdAt = LocalDateTime.now();
        this.isPreliminary = false;
    }

    public Bill(String billNumber, Patient patient, LocalDateTime billDate) {
        this();
        this.billNumber = billNumber;
        this.patient = patient;
        this.billDate = billDate != null ? billDate : LocalDateTime.now();
    }

    public void addItem(BillItem item) {
        if (item != null) {
            item.setBill(this);
            this.items.add(item);
            calculateTotals();
        }
    }

    public void removeItem(BillItem item) {
        if (item != null) {
            this.items.remove(item);
            item.setBill(null);
            calculateTotals();
        }
    }

    /**
     * Recomputes subtotal, total, and balance using server-side BigDecimal arithmetic.
     */
    public void calculateTotals() {
        BigDecimal sum = BigDecimal.ZERO;
        if (this.items != null) {
            for (BillItem item : this.items) {
                if (item.getLineTotal() == null) {
                    item.calculateLineTotal();
                }
                sum = sum.add(item.getLineTotal());
            }
        }
        this.subtotal = sum.setScale(2, RoundingMode.HALF_UP);

        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.taxAmount == null) {
            this.taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // total = subtotal - discount + tax (never less than 0)
        BigDecimal net = this.subtotal.subtract(this.discountAmount).add(this.taxAmount);
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            net = BigDecimal.ZERO;
        }
        this.totalAmount = net.setScale(2, RoundingMode.HALF_UP);

        BigDecimal bal = this.totalAmount.subtract(this.paidAmount);
        if (bal.compareTo(BigDecimal.ZERO) < 0) {
            bal = BigDecimal.ZERO;
        }
        this.balanceAmount = bal.setScale(2, RoundingMode.HALF_UP);

        updateStatusFromBalance();
    }

    public void updateStatusFromBalance() {
        if (this.totalAmount != null && this.paidAmount != null) {
            if (this.paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                this.status = BillStatus.UNPAID;
            } else if (this.paidAmount.compareTo(this.totalAmount) >= 0) {
                this.status = BillStatus.PAID;
                this.balanceAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else {
                this.status = BillStatus.PARTIALLY_PAID;
            }
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.billDate == null) {
            this.billDate = LocalDateTime.now();
        }
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateTotals();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber != null ? billNumber.trim() : null;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public Admission getAdmission() {
        return admission;
    }

    public void setAdmission(Admission admission) {
        this.admission = admission;
    }

    public LocalDateTime getBillDate() {
        return billDate;
    }

    public void setBillDate(LocalDateTime billDate) {
        this.billDate = billDate;
    }

    public BillStatus getStatus() {
        return status;
    }

    public void setStatus(BillStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount != null ? taxAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public void setBalanceAmount(BigDecimal balanceAmount) {
        this.balanceAmount = balanceAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    public boolean isPreliminary() {
        return isPreliminary;
    }

    public void setPreliminary(boolean preliminary) {
        isPreliminary = preliminary;
    }

    public List<BillItem> getItems() {
        return items;
    }

    public void setItems(List<BillItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        if (this.items != null) {
            for (BillItem item : this.items) {
                item.setBill(this);
            }
        }
        calculateTotals();
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments != null ? payments : new ArrayList<>();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
