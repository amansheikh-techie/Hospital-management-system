package com.hospital.hospital_management.model;

/**
 * Enumeration representing the status of an inpatient admission.
 */
public enum AdmissionStatus {
    ACTIVE("Active", "primary"),
    DISCHARGED("Discharged", "secondary");

    private final String displayName;
    private final String badgeClass;

    AdmissionStatus(String displayName, String badgeClass) {
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
