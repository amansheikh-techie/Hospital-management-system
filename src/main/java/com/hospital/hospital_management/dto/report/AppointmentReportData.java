package com.hospital.hospital_management.dto.report;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AppointmentReportData {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long doctorId;
    private AppointmentStatus status;

    private List<Appointment> appointments = new ArrayList<>();
    private long totalAppointments;
    private long scheduledCount;
    private long completedCount;
    private long cancelledCount;

    public AppointmentReportData() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public List<Appointment> getAppointments() { return appointments; }
    public void setAppointments(List<Appointment> appointments) { this.appointments = appointments; }

    public long getTotalAppointments() { return totalAppointments; }
    public void setTotalAppointments(long totalAppointments) { this.totalAppointments = totalAppointments; }

    public long getScheduledCount() { return scheduledCount; }
    public void setScheduledCount(long scheduledCount) { this.scheduledCount = scheduledCount; }

    public long getCompletedCount() { return completedCount; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }

    public long getCancelledCount() { return cancelledCount; }
    public void setCancelledCount(long cancelledCount) { this.cancelledCount = cancelledCount; }
}
