package com.hospital.hospital_management.model;

/**
 * Enumeration representing the category/type of a hospital bed.
 */
public enum BedType {
    GENERAL("General Ward"),
    SEMI_PRIVATE("Semi-Private"),
    PRIVATE("Private Room"),
    ICU("ICU"),
    EMERGENCY("Emergency");

    private final String displayName;

    BedType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
