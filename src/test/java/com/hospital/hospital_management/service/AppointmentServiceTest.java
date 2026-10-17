package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Appointment;
import com.hospital.hospital_management.model.AppointmentStatus;
import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.repository.AppointmentRepository;
import com.hospital.hospital_management.repository.DoctorRepository;
import com.hospital.hospital_management.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient samplePatient;
    private Doctor sampleDoctor;
    private Appointment sampleAppointment;
    private LocalDate appointmentDate;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient("Amit", "Patel", 30, "Male", "O+",
                "9876543210", "amit@example.com", "Address", "None");
        samplePatient.setId(1L);

        sampleDoctor = new Doctor("Rohan", "Mehra", "Cardiologist", "Cardiology",
                "9876500000", "rohan@hospital.com", 700.0, "Mon-Fri");
        sampleDoctor.setId(2L);

        appointmentDate = LocalDate.now().plusDays(1);

        sampleAppointment = new Appointment(samplePatient, sampleDoctor, appointmentDate,
                LocalTime.of(10, 0), LocalTime.of(10, 30), "Cardiology follow-up");
        sampleAppointment.setId(10L);
    }

    @Test
    void testBookAppointmentSuccessful() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);
        when(appointmentRepository.findOverlappingAppointments(2L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(10, 30), null))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        Appointment booked = appointmentService.bookAppointment(sampleAppointment);

        assertNotNull(booked);
        assertEquals(10L, booked.getId());
        assertEquals(AppointmentStatus.SCHEDULED, booked.getStatus());
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testBookAppointmentMissingPatientThrowsException() {
        sampleAppointment.setPatient(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(sampleAppointment);
        });
        assertTrue(ex.getMessage().contains("patient must be selected"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointmentMissingDoctorThrowsException() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        sampleAppointment.setDoctor(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(sampleAppointment);
        });
        assertTrue(ex.getMessage().contains("doctor must be selected"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointmentEndTimeNotAfterStartTimeThrowsException() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        // End time is before start time (10:00 to 09:30)
        sampleAppointment.setStartTime(LocalTime.of(10, 0));
        sampleAppointment.setEndTime(LocalTime.of(9, 30));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(sampleAppointment);
        });
        assertTrue(ex.getMessage().contains("must be after start time"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointmentEndTimeEqualStartTimeThrowsException() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        // Same start and end time (10:00 to 10:00)
        sampleAppointment.setStartTime(LocalTime.of(10, 0));
        sampleAppointment.setEndTime(LocalTime.of(10, 0));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(sampleAppointment);
        });
        assertTrue(ex.getMessage().contains("must be after start time"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointmentOverlappingDoctorThrowsConflictException() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        Appointment existingConflict = new Appointment(samplePatient, sampleDoctor, appointmentDate,
                LocalTime.of(10, 15), LocalTime.of(10, 45), "Existing booking");
        existingConflict.setId(99L);

        when(appointmentRepository.findOverlappingAppointments(2L, appointmentDate, LocalTime.of(10, 0), LocalTime.of(10, 30), null))
                .thenReturn(List.of(existingConflict));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            appointmentService.bookAppointment(sampleAppointment);
        });

        assertTrue(ex.getMessage().contains("Doctor schedule conflict"));
        assertTrue(ex.getMessage().contains("Dr. Rohan Mehra"));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void testBookAppointmentNonOverlappingBeforeSuccess() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        // New appointment is 09:00 - 09:30, existing was 10:00 - 10:30 (no overlap)
        sampleAppointment.setStartTime(LocalTime.of(9, 0));
        sampleAppointment.setEndTime(LocalTime.of(9, 30));

        when(appointmentRepository.findOverlappingAppointments(2L, appointmentDate, LocalTime.of(9, 0), LocalTime.of(9, 30), null))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        Appointment booked = appointmentService.bookAppointment(sampleAppointment);

        assertNotNull(booked);
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testBookAppointmentNonOverlappingAfterSuccess() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        // New appointment is 10:30 - 11:00 (exact boundary touch, no interval overlap)
        sampleAppointment.setStartTime(LocalTime.of(10, 30));
        sampleAppointment.setEndTime(LocalTime.of(11, 0));

        when(appointmentRepository.findOverlappingAppointments(2L, appointmentDate, LocalTime.of(10, 30), LocalTime.of(11, 0), null))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        Appointment booked = appointmentService.bookAppointment(sampleAppointment);

        assertNotNull(booked);
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testRescheduleSelfExclusionSuccess() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(sampleAppointment));
        when(patientRepository.existsById(1L)).thenReturn(true);
        when(doctorRepository.existsById(2L)).thenReturn(true);

        // Reschedule same appointment from 10:00-10:30 to 10:15-10:45
        Appointment updatedData = new Appointment(samplePatient, sampleDoctor, appointmentDate,
                LocalTime.of(10, 15), LocalTime.of(10, 45), "Shifted slot");

        // When excluding self (id=10), no other appointment overlaps
        when(appointmentRepository.findOverlappingAppointments(2L, appointmentDate, LocalTime.of(10, 15), LocalTime.of(10, 45), 10L))
                .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(sampleAppointment);

        Appointment result = appointmentService.updateAppointment(10L, updatedData);

        assertNotNull(result);
        verify(appointmentRepository, times(1)).findOverlappingAppointments(2L, appointmentDate, LocalTime.of(10, 15), LocalTime.of(10, 45), 10L);
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testCancelAppointmentSetsStatus() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(sampleAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointment(10L);

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testCompleteAppointmentSetsStatus() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(sampleAppointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment completed = appointmentService.completeAppointment(10L);

        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
        verify(appointmentRepository, times(1)).save(sampleAppointment);
    }

    @Test
    void testGetTodayAppointmentCount() {
        when(appointmentRepository.countByAppointmentDate(LocalDate.now())).thenReturn(3L);

        long count = appointmentService.getTodayAppointmentCount();

        assertEquals(3L, count);
        verify(appointmentRepository, times(1)).countByAppointmentDate(LocalDate.now());
    }

    @Test
    void testGetTotalAppointmentCount() {
        when(appointmentRepository.count()).thenReturn(15L);

        long count = appointmentService.getTotalAppointmentCount();

        assertEquals(15L, count);
        verify(appointmentRepository, times(1)).count();
    }
}
