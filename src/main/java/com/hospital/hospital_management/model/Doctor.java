package com.hospital.hospital_management.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Entity representing a doctor in the hospital.
 *
 * Mapped to the 'doctors' table in MySQL hospital_db.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Specialization is required")
    @Size(min = 2, max = 100, message = "Specialization must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String specialization;

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String department;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    @Column(name = "contact_number", nullable = false, length = 15)
    private String contactNumber;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    @Column(nullable = false, length = 100)
    private String email;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "1.0", message = "Consultation fee must be at least ₹1")
    @Max(value = 50000, message = "Consultation fee cannot exceed ₹50,000")
    @Column(name = "consultation_fee", nullable = false)
    private Double consultationFee;

    @NotBlank(message = "Available days are required")
    @Size(max = 100, message = "Available days description cannot exceed 100 characters")
    @Column(name = "available_days", nullable = false, length = 100)
    private String availableDays;

    @NotNull
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;

    // Default constructor for JPA
    public Doctor() {
        this.registrationDate = LocalDate.now();
    }

    public Doctor(String firstName, String lastName, String specialization, String department,
                  String contactNumber, String email, Double consultationFee, String availableDays) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.specialization = specialization;
        this.department = department;
        this.contactNumber = contactNumber;
        this.email = email;
        this.consultationFee = consultationFee;
        this.availableDays = availableDays;
        this.registrationDate = LocalDate.now();
    }

    // Helper methods
    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getDisplayName() {
        return "Dr. " + getFullName();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }
}
