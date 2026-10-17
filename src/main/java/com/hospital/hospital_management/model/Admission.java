package com.hospital.hospital_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * Entity representing an inpatient admission of a Patient to a Bed.
 *
 * Mapped to the 'admissions' table in MySQL hospital_db.
 */
@Entity
@Table(name = "admissions")
public class Admission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Please select a patient")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @NotNull(message = "Please select an available bed")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "bed_id", nullable = false)
    private Bed bed;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attending_doctor_id")
    private Doctor attendingDoctor;

    @NotNull(message = "Admission date and time is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "admission_date", nullable = false)
    private LocalDateTime admissionDate;

    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    @Column(name = "admission_reason", length = 255)
    private String admissionReason;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Size(max = 500, message = "Discharge notes cannot exceed 500 characters")
    @Column(name = "discharge_notes", length = 500)
    private String dischargeNotes;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Admission() {
        this.status = AdmissionStatus.ACTIVE;
        this.admissionDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public Admission(Patient patient, Bed bed, Doctor attendingDoctor,
                     LocalDateTime admissionDate, String admissionReason) {
        this();
        this.patient = patient;
        this.bed = bed;
        this.attendingDoctor = attendingDoctor;
        this.admissionDate = admissionDate != null ? admissionDate : LocalDateTime.now();
        this.admissionReason = admissionReason != null ? admissionReason.trim() : null;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = AdmissionStatus.ACTIVE;
        }
        if (this.admissionDate == null) {
            this.admissionDate = LocalDateTime.now();
        }
    }

    public boolean isActive() {
        return AdmissionStatus.ACTIVE.equals(this.status);
    }

    public boolean isDischarged() {
        return AdmissionStatus.DISCHARGED.equals(this.status);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Bed getBed() {
        return bed;
    }

    public void setBed(Bed bed) {
        this.bed = bed;
    }

    public Doctor getAttendingDoctor() {
        return attendingDoctor;
    }

    public void setAttendingDoctor(Doctor attendingDoctor) {
        this.attendingDoctor = attendingDoctor;
    }

    public LocalDateTime getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDateTime admissionDate) {
        this.admissionDate = admissionDate;
    }

    public String getAdmissionReason() {
        return admissionReason;
    }

    public void setAdmissionReason(String admissionReason) {
        this.admissionReason = admissionReason != null ? admissionReason.trim() : null;
    }

    public AdmissionStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionStatus status) {
        this.status = status;
    }

    public LocalDateTime getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(LocalDateTime dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public String getDischargeNotes() {
        return dischargeNotes;
    }

    public void setDischargeNotes(String dischargeNotes) {
        this.dischargeNotes = dischargeNotes != null ? dischargeNotes.trim() : null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
