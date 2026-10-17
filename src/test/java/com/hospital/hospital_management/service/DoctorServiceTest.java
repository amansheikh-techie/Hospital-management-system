package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Doctor;
import com.hospital.hospital_management.repository.DoctorRepository;
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
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorService doctorService;

    private Doctor sampleDoctor;

    @BeforeEach
    void setUp() {
        sampleDoctor = new Doctor(
                "Ananya", "Deshmukh", "Cardiologist", "Cardiology",
                "9876543210", "ananya@hospital.com", 600.0, "Mon, Wed, Fri"
        );
        sampleDoctor.setId(1L);
    }

    @Test
    void testSaveDoctor() {
        when(doctorRepository.save(any(Doctor.class))).thenReturn(sampleDoctor);

        Doctor saved = doctorService.saveDoctor(sampleDoctor);

        assertNotNull(saved);
        assertEquals("Ananya", saved.getFirstName());
        assertEquals("Deshmukh", saved.getLastName());
        assertEquals("Dr. Ananya Deshmukh", saved.getDisplayName());
        verify(doctorRepository, times(1)).save(sampleDoctor);
    }

    @Test
    void testGetAllDoctors() {
        when(doctorRepository.findAllByOrderByIdDesc()).thenReturn(List.of(sampleDoctor));

        List<Doctor> list = doctorService.getAllDoctors();

        assertEquals(1, list.size());
        assertEquals("Dr. Ananya Deshmukh", list.get(0).getDisplayName());
        verify(doctorRepository, times(1)).findAllByOrderByIdDesc();
    }

    @Test
    void testSearchDoctorsWithKeyword() {
        when(doctorRepository.searchDoctors("Cardio")).thenReturn(List.of(sampleDoctor));

        List<Doctor> results = doctorService.searchDoctors("Cardio");

        assertEquals(1, results.size());
        assertEquals("Cardiology", results.get(0).getDepartment());
        verify(doctorRepository, times(1)).searchDoctors("Cardio");
    }

    @Test
    void testSearchDoctorsEmptyKeywordReturnsAll() {
        when(doctorRepository.findAllByOrderByIdDesc()).thenReturn(List.of(sampleDoctor));

        List<Doctor> results = doctorService.searchDoctors("  ");

        assertEquals(1, results.size());
        verify(doctorRepository, times(1)).findAllByOrderByIdDesc();
    }

    @Test
    void testGetDoctorByIdFound() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));

        Optional<Doctor> result = doctorService.getDoctorById(1L);

        assertTrue(result.isPresent());
        assertEquals("Ananya", result.get().getFirstName());
    }

    @Test
    void testGetDoctorByIdNotFound() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Doctor> result = doctorService.getDoctorById(99L);

        assertFalse(result.isPresent());
    }

    @Test
    void testUpdateDoctorSuccess() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        Doctor updated = new Doctor(
                "Ananya", "Deshmukh", "Senior Cardiologist", "Cardiology",
                "9876543210", "ananya.d@hospital.com", 800.0, "Mon, Tue, Thu"
        );

        Doctor result = doctorService.updateDoctor(1L, updated);

        assertEquals("Senior Cardiologist", result.getSpecialization());
        assertEquals(800.0, result.getConsultationFee());
        verify(doctorRepository, times(1)).save(any(Doctor.class));
    }

    @Test
    void testUpdateDoctorNotFoundThrowsException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            doctorService.updateDoctor(99L, sampleDoctor);
        });
    }

    @Test
    void testDeleteDoctorSuccess() {
        when(doctorRepository.existsById(1L)).thenReturn(true);
        doNothing().when(doctorRepository).deleteById(1L);

        boolean deleted = doctorService.deleteDoctor(1L);

        assertTrue(deleted);
        verify(doctorRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteDoctorNotFound() {
        when(doctorRepository.existsById(99L)).thenReturn(false);

        boolean deleted = doctorService.deleteDoctor(99L);

        assertFalse(deleted);
        verify(doctorRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetTotalDoctorCount() {
        when(doctorRepository.count()).thenReturn(5L);

        long count = doctorService.getTotalDoctorCount();

        assertEquals(5L, count);
        verify(doctorRepository, times(1)).count();
    }

    @Test
    void testGetAllDepartments() {
        when(doctorRepository.findDistinctDepartments()).thenReturn(List.of("Cardiology", "Neurology"));

        List<String> departments = doctorService.getAllDepartments();

        assertEquals(2, departments.size());
        assertTrue(departments.contains("Cardiology"));
        verify(doctorRepository, times(1)).findDistinctDepartments();
    }
}
