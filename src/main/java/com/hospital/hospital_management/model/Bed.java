package com.hospital.hospital_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a hospital bed.
 *
 * Mapped to the 'beds' table in MySQL hospital_db.
 */
@Entity
@Table(name = "beds", uniqueConstraints = {
        @UniqueConstraint(name = "uk_bed_number", columnNames = "bed_number")
})
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Bed number is required")
    @Size(max = 20, message = "Bed number cannot exceed 20 characters")
    @Column(name = "bed_number", nullable = false, unique = true, length = 20)
    private String bedNumber;

    @NotBlank(message = "Ward name is required")
    @Size(max = 60, message = "Ward name cannot exceed 60 characters")
    @Column(nullable = false, length = 60)
    private String ward;

    @NotNull(message = "Bed type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "bed_type", nullable = false, length = 30)
    private BedType bedType;

    @Size(max = 30, message = "Floor cannot exceed 30 characters")
    @Column(length = 30)
    private String floor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BedStatus status;

    @DecimalMin(value = "0.0", inclusive = true, message = "Daily charge must be zero or positive")
    @Column(name = "daily_charge", precision = 10, scale = 2)
    private BigDecimal dailyCharge;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Column(length = 255)
    private String description;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Bed() {
        this.status = BedStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.dailyCharge = BigDecimal.ZERO;
    }

    public Bed(String bedNumber, String ward, BedType bedType, String floor,
               BigDecimal dailyCharge, String description) {
        this();
        this.bedNumber = bedNumber != null ? bedNumber.trim() : null;
        this.ward = ward != null ? ward.trim() : null;
        this.bedType = bedType;
        this.floor = floor != null ? floor.trim() : null;
        this.dailyCharge = dailyCharge != null ? dailyCharge : BigDecimal.ZERO;
        this.description = description != null ? description.trim() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = BedStatus.AVAILABLE;
        }
        if (this.dailyCharge == null) {
            this.dailyCharge = BigDecimal.ZERO;
        }
    }

    public boolean isAvailable() {
        return BedStatus.AVAILABLE.equals(this.status);
    }

    public boolean isOccupied() {
        return BedStatus.OCCUPIED.equals(this.status);
    }

    public boolean isUnderMaintenance() {
        return BedStatus.MAINTENANCE.equals(this.status);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBedNumber() {
        return bedNumber;
    }

    public void setBedNumber(String bedNumber) {
        this.bedNumber = bedNumber != null ? bedNumber.trim() : null;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward != null ? ward.trim() : null;
    }

    public BedType getBedType() {
        return bedType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor != null ? floor.trim() : null;
    }

    public BedStatus getStatus() {
        return status;
    }

    public void setStatus(BedStatus status) {
        this.status = status;
    }

    public BigDecimal getDailyCharge() {
        return dailyCharge;
    }

    public void setDailyCharge(BigDecimal dailyCharge) {
        this.dailyCharge = dailyCharge;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
