package com.hospital.hospital_management.dto.report;

import com.hospital.hospital_management.model.Patient;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatientReportData {
    private LocalDate startDate;
    private LocalDate endDate;
    private String bloodGroup;
    private String gender;

    private List<Patient> patients = new ArrayList<>();
    private long totalPatients;
    private Map<String, Long> genderBreakdown = new LinkedHashMap<>();
    private Map<String, Long> bloodGroupBreakdown = new LinkedHashMap<>();

    public PatientReportData() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public List<Patient> getPatients() { return patients; }
    public void setPatients(List<Patient> patients) { this.patients = patients; }

    public long getTotalPatients() { return totalPatients; }
    public void setTotalPatients(long totalPatients) { this.totalPatients = totalPatients; }

    public Map<String, Long> getGenderBreakdown() { return genderBreakdown; }
    public void setGenderBreakdown(Map<String, Long> genderBreakdown) { this.genderBreakdown = genderBreakdown; }

    public Map<String, Long> getBloodGroupBreakdown() { return bloodGroupBreakdown; }
    public void setBloodGroupBreakdown(Map<String, Long> bloodGroupBreakdown) { this.bloodGroupBreakdown = bloodGroupBreakdown; }
}
