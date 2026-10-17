package com.hospital.hospital_management.model;

/**
 * Status of an appointment in the hospital.
 */
public enum AppointmentStatus {
    SCHEDULED("Scheduled", "bg-primary-subtle text-primary border-primary-subtle"),
    COMPLETED("Completed", "bg-success-subtle text-success border-success-subtle"),
    CANCELLED("Cancelled", "bg-danger-subtle text-danger border-danger-subtle");

    private final String displayName;
    private final String badgeClass;

    AppointmentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
