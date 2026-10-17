package com.hospital.hospital_management.model;

/**
 * Roles for staff members in the Hospital Management System.
 */
public enum Role {
    ROLE_ADMIN("Administrator", "Full system access and user management"),
    ROLE_DOCTOR("Doctor", "Clinical access, consultations, and patient care"),
    ROLE_RECEPTIONIST("Receptionist", "Patient registration, appointment scheduling, and bed admissions"),
    ROLE_BILLING_STAFF("Billing Staff", "Invoice generation, payment collection, and financial records");

    private final String displayName;
    private final String description;

    Role(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
