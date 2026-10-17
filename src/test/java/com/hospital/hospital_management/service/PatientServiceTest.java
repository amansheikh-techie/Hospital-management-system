package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Patient;
import com.hospital.hospital_management.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient samplePatient;

    @BeforeEach
    void setUp() {
        samplePatient = new Patient(
                "Aman", "Verma", 25, "Male", "B+",
                "9876543210", "aman@example.com",
                "123 Park Street, Mumbai", "No prior conditions"
        );
        samplePatient.setId(1L);
    }

    @Test
    void testSavePatient() {
        when(patientRepository.save(any(Patient.class))).thenReturn(samplePatient);

        Patient saved = patientService.savePatient(samplePatient);

        assertNotNull(saved);
        assertEquals("Aman", saved.getFirstName());
        assertEquals("Verma", saved.getLastName());
        assertEquals("Aman Verma", saved.getFullName());
        verify(patientRepository, times(1)).save(samplePatient);
    }

    @Test
    void testGetAllPatients() {
        when(patientRepository.findAllByOrderByIdDesc()).thenReturn(List.of(samplePatient));

        List<Patient> list = patientService.getAllPatients();

        assertEquals(1, list.size());
        assertEquals("Aman Verma", list.get(0).getFullName());
        verify(patientRepository, times(1)).findAllByOrderByIdDesc();
    }

    @Test
    void testSearchPatientsWithKeyword() {
        when(patientRepository.searchPatients("Aman")).thenReturn(List.of(samplePatient));

        List<Patient> results = patientService.searchPatients("Aman");

        assertEquals(1, results.size());
        assertEquals("Aman", results.get(0).getFirstName());
        verify(patientRepository, times(1)).searchPatients("Aman");
    }

    @Test
    void testSearchPatientsWithEmptyKeywordReturnsAll() {
        when(patientRepository.findAllByOrderByIdDesc()).thenReturn(List.of(samplePatient));

        List<Patient> results = patientService.searchPatients("   ");

        assertEquals(1, results.size());
        verify(patientRepository, times(1)).findAllByOrderByIdDesc();
    }

    @Test
    void testGetPatientByIdFound() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));

        Optional<Patient> result = patientService.getPatientById(1L);

        assertTrue(result.isPresent());
        assertEquals("Aman", result.get().getFirstName());
    }

    @Test
    void testGetPatientByIdNotFound() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Patient> result = patientService.getPatientById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdatePatientSuccess() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(samplePatient));
        when(patientRepository.save(any(Patient.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient updatedData = new Patient(
                "Aman", "Sharma", 26, "Male", "B+",
                "9998887776", "amansharma@example.com",
                "456 New Road, Pune", "Mild asthma"
        );

        Patient result = patientService.updatePatient(1L, updatedData);

        assertEquals("Sharma", result.getLastName());
        assertEquals(26, result.getAge());
        assertEquals("Mild asthma", result.getMedicalHistory());
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testUpdatePatientNotFoundThrowsException() {
        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            patientService.updatePatient(99L, samplePatient);
        });
    }

    @Test
    void testDeletePatientSuccess() {
        when(patientRepository.existsById(1L)).thenReturn(true);
        doNothing().when(patientRepository).deleteById(1L);

        boolean deleted = patientService.deletePatient(1L);

        assertTrue(deleted);
        verify(patientRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeletePatientNotFoundReturnsFalse() {
        when(patientRepository.existsById(99L)).thenReturn(false);

        boolean deleted = patientService.deletePatient(99L);

        assertFalse(deleted);
        verify(patientRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetTotalPatientCount() {
        when(patientRepository.count()).thenReturn(10L);

        long count = patientService.getTotalPatientCount();

        assertEquals(10L, count);
        verify(patientRepository, times(1)).count();
    }
}
