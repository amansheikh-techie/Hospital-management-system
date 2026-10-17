package com.hospital.hospital_management.dto.report;

import com.hospital.hospital_management.model.Bed;
import java.util.ArrayList;
import java.util.List;

public class BedOccupancyReportData {
    private long totalBeds;
    private long availableBeds;
    private long occupiedBeds;
    private long maintenanceBeds;
    private double overallOccupancyRate;
    private List<WardOccupancySummary> wardSummaries = new ArrayList<>();
    private List<BedTypeSummary> typeSummaries = new ArrayList<>();
    private List<Bed> beds = new ArrayList<>();

    public BedOccupancyReportData() {}

    public long getTotalBeds() { return totalBeds; }
    public void setTotalBeds(long totalBeds) { this.totalBeds = totalBeds; }

    public long getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(long availableBeds) { this.availableBeds = availableBeds; }

    public long getOccupiedBeds() { return occupiedBeds; }
    public void setOccupiedBeds(long occupiedBeds) { this.occupiedBeds = occupiedBeds; }

    public long getMaintenanceBeds() { return maintenanceBeds; }
    public void setMaintenanceBeds(long maintenanceBeds) { this.maintenanceBeds = maintenanceBeds; }

    public double getOverallOccupancyRate() { return overallOccupancyRate; }
    public void setOverallOccupancyRate(double overallOccupancyRate) { this.overallOccupancyRate = overallOccupancyRate; }

    public List<WardOccupancySummary> getWardSummaries() { return wardSummaries; }
    public void setWardSummaries(List<WardOccupancySummary> wardSummaries) { this.wardSummaries = wardSummaries; }

    public List<BedTypeSummary> getTypeSummaries() { return typeSummaries; }
    public void setTypeSummaries(List<BedTypeSummary> typeSummaries) { this.typeSummaries = typeSummaries; }

    public List<Bed> getBeds() { return beds; }
    public void setBeds(List<Bed> beds) { this.beds = beds; }
}
