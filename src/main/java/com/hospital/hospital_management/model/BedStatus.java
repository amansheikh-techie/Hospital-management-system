package com.hospital.hospital_management.model;

/**
 * Enumeration representing the operational status of a hospital bed.
 */
public enum BedStatus {
    AVAILABLE("Available", "success"),
    OCCUPIED("Occupied", "danger"),
    MAINTENANCE("Maintenance", "warning");

    private final String displayName;
    private final String badgeClass;

    BedStatus(String displayName, String badgeClass) {
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
