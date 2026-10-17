package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.*;
import com.hospital.hospital_management.repository.AdmissionRepository;
import com.hospital.hospital_management.repository.BedRepository;
import com.hospital.hospital_management.repository.DoctorRepository;
import com.hospital.hospital_management.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionRepository admissionRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private AdmissionService admissionService;

    private Patient samplePatient;
    private Bed sampleBed;
    private Doctor sampleDoctor;
    private Admission sampleAdmission;
    private LocalDateTime admissionTime;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient("Amit", "Patel", 30, "Male", "O+",
                "9876543210", "amit@example.com", "Address", "None");
        samplePatient.setId(1L);

        sampleBed = new Bed("B-101", "General Ward A", BedType.GENERAL, "1st Floor",
                new BigDecimal("500.00"), "Window side");
        sampleBed.setId(10L);
        sampleBed.setStatus(BedStatus.AVAILABLE);

        sampleDoctor = new Doctor("Rohan", "Mehra", "Cardiologist", "Cardiology",
                "9876500000", "rohan@hospital.com", 700.0, "Mon-Fri");
        sampleDoctor.setId(20L);

        admissionTime = LocalDateTime.now().minusDays(1);
        sampleAdmission = new Admission(samplePatient, sampleBed, sampleDoctor, admissionTime, "Observation");
        sampleAdmission.setId(100L);
    }

    @Test
    void admitPatient_successful() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(bedRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedIdAndStatus(10L, AdmissionStatus.ACTIVE)).thenReturn(false);
        when(doctorRepository.findById(20L)).thenReturn(Optional.of(sampleDoctor));
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> i.getArgument(0));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> {
            Admission a = i.getArgument(0);
            a.setId(100L);
            return a;
        });

        Admission admission = admissionService.admitPatient(1L, 10L, 20L, admissionTime, "Observation");

        assertNotNull(admission);
        assertEquals(AdmissionStatus.ACTIVE, admission.getStatus());
        assertEquals(samplePatient, admission.getPatient());
        assertEquals(sampleBed, admission.getBed());
        assertEquals(BedStatus.OCCUPIED, sampleBed.getStatus());

        verify(bedRepository, times(1)).save(sampleBed);
        verify(admissionRepository, times(1)).save(any(Admission.class));
    }

    @Test
    void admitPatient_patientNotFound_throwsException() {
        when(patientRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                admissionService.admitPatient(999L, 10L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("Patient not found"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void admitPatient_patientAlreadyAdmitted_throwsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE))
                .thenReturn(Optional.of(sampleAdmission));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                admissionService.admitPatient(1L, 10L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("already has an active admission"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void admitPatient_bedNotFound_throwsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(bedRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                admissionService.admitPatient(1L, 999L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("Bed not found"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void admitPatient_bedNotAvailableOccupied_throwsException() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(bedRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBed));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                admissionService.admitPatient(1L, 10L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("is not available"));
        verify(admissionRepository, never()).save(any(Admission.class));
    }

    @Test
    void admitPatient_bedNotAvailableMaintenance_throwsException() {
        sampleBed.setStatus(BedStatus.MAINTENANCE);
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(bedRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBed));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                admissionService.admitPatient(1L, 10L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("is not available"));
        verify(admissionRepository, never()).save(any(Admission.class));
    }

    @Test
    void admitPatient_duplicateActiveAdmissionOnBed_throwsException() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(admissionRepository.findByPatientIdAndStatus(1L, AdmissionStatus.ACTIVE)).thenReturn(Optional.empty());
        when(bedRepository.findByIdWithLock(10L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedIdAndStatus(10L, AdmissionStatus.ACTIVE)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                admissionService.admitPatient(1L, 10L, null, admissionTime, "Observation"));

        assertTrue(ex.getMessage().contains("already occupied by an active admission"));
        verify(admissionRepository, never()).save(any(Admission.class));
    }

    @Test
    void dischargePatient_successful() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        sampleAdmission.setStatus(AdmissionStatus.ACTIVE);
        when(admissionRepository.findById(100L)).thenReturn(Optional.of(sampleAdmission));
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> i.getArgument(0));
        when(admissionRepository.save(any(Admission.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime dischargeTime = LocalDateTime.now();
        Admission discharged = admissionService.dischargePatient(100L, dischargeTime, "Patient recovered");

        assertEquals(AdmissionStatus.DISCHARGED, discharged.getStatus());
        assertEquals(dischargeTime, discharged.getDischargeDate());
        assertEquals("Patient recovered", discharged.getDischargeNotes());
        assertEquals(BedStatus.AVAILABLE, sampleBed.getStatus());

        verify(bedRepository, times(1)).save(sampleBed);
        verify(admissionRepository, times(1)).save(sampleAdmission);
    }

    @Test
    void dischargePatient_nonexistentAdmission_throwsException() {
        when(admissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                admissionService.dischargePatient(999L, LocalDateTime.now(), "Notes"));

        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void dischargePatient_alreadyDischarged_throwsException() {
        sampleAdmission.setStatus(AdmissionStatus.DISCHARGED);
        when(admissionRepository.findById(100L)).thenReturn(Optional.of(sampleAdmission));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                admissionService.dischargePatient(100L, LocalDateTime.now(), "Notes"));

        assertTrue(ex.getMessage().contains("already marked as Discharged"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void dischargePatient_dischargeBeforeAdmissionDate_throwsException() {
        sampleAdmission.setStatus(AdmissionStatus.ACTIVE);
        when(admissionRepository.findById(100L)).thenReturn(Optional.of(sampleAdmission));

        LocalDateTime earlier = admissionTime.minusHours(2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                admissionService.dischargePatient(100L, earlier, "Notes"));

        assertTrue(ex.getMessage().contains("cannot be earlier than admission"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void searchAndFilterAdmissions_delegatesToRepository() {
        when(admissionRepository.searchAndFilterAdmissions("Amit", AdmissionStatus.ACTIVE, "General"))
                .thenReturn(Collections.singletonList(sampleAdmission));

        List<Admission> result = admissionService.searchAndFilterAdmissions("  Amit  ", AdmissionStatus.ACTIVE, "  General  ");

        assertEquals(1, result.size());
        verify(admissionRepository, times(1)).searchAndFilterAdmissions("Amit", AdmissionStatus.ACTIVE, "General");
    }

    @Test
    void dashboardAdmissionCounts() {
        when(admissionRepository.countByStatus(AdmissionStatus.ACTIVE)).thenReturn(5L);
        when(admissionRepository.countByAdmissionDateBetween(any(), any())).thenReturn(2L);

        assertEquals(5L, admissionService.getActiveAdmissionCount());
        assertEquals(2L, admissionService.getTodayAdmissionCount());
    }
}
