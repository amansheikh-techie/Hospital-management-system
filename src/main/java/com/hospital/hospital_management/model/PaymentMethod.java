package com.hospital.hospital_management.model;

/**
 * Supported payment methods for hospital billing.
 */
public enum PaymentMethod {
    CASH("Cash"),
    CARD("Debit / Credit Card"),
    BANK_TRANSFER("Bank Transfer / NEFT"),
    UPI("UPI / QR Code");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
