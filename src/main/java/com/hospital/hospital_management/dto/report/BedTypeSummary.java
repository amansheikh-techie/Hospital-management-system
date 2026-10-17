package com.hospital.hospital_management.dto.report;

import com.hospital.hospital_management.model.BedType;
import java.math.BigDecimal;

public class BedTypeSummary {
    private BedType bedType;
    private long totalBeds;
    private long availableBeds;
    private long occupiedBeds;
    private BigDecimal averageDailyCharge;

    public BedTypeSummary() {}

    public BedTypeSummary(BedType bedType, long totalBeds, long availableBeds, long occupiedBeds, BigDecimal averageDailyCharge) {
        this.bedType = bedType;
        this.totalBeds = totalBeds;
        this.availableBeds = availableBeds;
        this.occupiedBeds = occupiedBeds;
        this.averageDailyCharge = averageDailyCharge;
    }

    public BedType getBedType() { return bedType; }
    public void setBedType(BedType bedType) { this.bedType = bedType; }

    public long getTotalBeds() { return totalBeds; }
    public void setTotalBeds(long totalBeds) { this.totalBeds = totalBeds; }

    public long getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(long availableBeds) { this.availableBeds = availableBeds; }

    public long getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(long occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public BigDecimal getAverageDailyCharge() { return averageDailyCharge; }
    public void setAverageDailyCharge(BigDecimal averageDailyCharge) { this.averageDailyCharge = averageDailyCharge; }
}
