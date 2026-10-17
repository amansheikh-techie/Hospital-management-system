package com.hospital.hospital_management.model;

/**
 * Enumeration representing the settlement status of an invoice/bill.
 */
public enum BillStatus {
    UNPAID("Unpaid", "danger"),
    PARTIALLY_PAID("Partially Paid", "warning"),
    PAID("Paid", "success");

    private final String displayName;
    private final String badgeClass;

    BillStatus(String displayName, String badgeClass) {
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
