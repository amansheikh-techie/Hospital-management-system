package com.hospital.hospital_management.service;

import com.hospital.hospital_management.model.Bed;
import com.hospital.hospital_management.model.BedStatus;
import com.hospital.hospital_management.model.BedType;
import com.hospital.hospital_management.repository.AdmissionRepository;
import com.hospital.hospital_management.repository.BedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BedServiceTest {

    @Mock
    private BedRepository bedRepository;

    @Mock
    private AdmissionRepository admissionRepository;

    @InjectMocks
    private BedService bedService;

    private Bed sampleBed;

    @BeforeEach
    void setUp() {
        sampleBed = new Bed("B-101", "General Ward A", BedType.GENERAL, "1st Floor",
                new BigDecimal("500.00"), "Near nurse station");
        sampleBed.setId(1L);
    }

    @Test
    void createBed_successful() {
        when(bedRepository.existsByBedNumber("B-101")).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> {
            Bed b = i.getArgument(0);
            b.setId(1L);
            return b;
        });

        Bed created = bedService.createBed(sampleBed);

        assertNotNull(created);
        assertEquals("B-101", created.getBedNumber());
        assertEquals(BedStatus.AVAILABLE, created.getStatus());
        verify(bedRepository, times(1)).save(sampleBed);
    }

    @Test
    void createBed_duplicateBedNumber_throwsException() {
        when(bedRepository.existsByBedNumber("B-101")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bedService.createBed(sampleBed));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void createBed_blankBedNumber_throwsException() {
        sampleBed.setBedNumber("   ");

        assertThrows(IllegalArgumentException.class, () -> bedService.createBed(sampleBed));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void updateBed_successful() {
        Bed updated = new Bed("B-101-A", "General Ward B", BedType.SEMI_PRIVATE, "2nd Floor",
                new BigDecimal("800.00"), "Updated notes");
        updated.setStatus(BedStatus.AVAILABLE);

        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(bedRepository.existsByBedNumberAndIdNot("B-101-A", 1L)).thenReturn(false);
        when(admissionRepository.existsByBedIdAndStatus(1L, com.hospital.hospital_management.model.AdmissionStatus.ACTIVE)).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> i.getArgument(0));

        Bed result = bedService.updateBed(1L, updated);

        assertEquals("B-101-A", result.getBedNumber());
        assertEquals("General Ward B", result.getWard());
        assertEquals(BedType.SEMI_PRIVATE, result.getBedType());
        verify(bedRepository, times(1)).save(sampleBed);
    }

    @Test
    void updateBed_duplicateBedNumber_throwsException() {
        Bed updated = new Bed("B-102", "General Ward A", BedType.GENERAL, "1st Floor",
                new BigDecimal("500.00"), "");

        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(bedRepository.existsByBedNumberAndIdNot("B-102", 1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                bedService.updateBed(1L, updated));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void updateBed_occupiedBedManualAvailable_throwsException() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        Bed updated = new Bed("B-101", "General Ward A", BedType.GENERAL, "1st Floor",
                new BigDecimal("500.00"), "");
        updated.setStatus(BedStatus.AVAILABLE);

        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(bedRepository.existsByBedNumberAndIdNot("B-101", 1L)).thenReturn(false);
        when(admissionRepository.existsByBedIdAndStatus(1L, com.hospital.hospital_management.model.AdmissionStatus.ACTIVE)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bedService.updateBed(1L, updated));

        assertTrue(ex.getMessage().contains("active patient admission"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void markUnderMaintenance_successful() {
        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedIdAndStatus(1L, com.hospital.hospital_management.model.AdmissionStatus.ACTIVE)).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> i.getArgument(0));

        Bed result = bedService.markUnderMaintenance(1L);

        assertEquals(BedStatus.MAINTENANCE, result.getStatus());
        verify(bedRepository, times(1)).save(sampleBed);
    }

    @Test
    void markUnderMaintenance_occupiedBed_throwsException() {
        sampleBed.setStatus(BedStatus.OCCUPIED);
        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedIdAndStatus(1L, com.hospital.hospital_management.model.AdmissionStatus.ACTIVE)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bedService.markUnderMaintenance(1L));

        assertTrue(ex.getMessage().contains("currently occupied"));
        verify(bedRepository, never()).save(any(Bed.class));
    }

    @Test
    void returnToAvailable_successful() {
        sampleBed.setStatus(BedStatus.MAINTENANCE);
        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedIdAndStatus(1L, com.hospital.hospital_management.model.AdmissionStatus.ACTIVE)).thenReturn(false);
        when(bedRepository.save(any(Bed.class))).thenAnswer(i -> i.getArgument(0));

        Bed result = bedService.returnToAvailable(1L);

        assertEquals(BedStatus.AVAILABLE, result.getStatus());
        verify(bedRepository, times(1)).save(sampleBed);
    }

    @Test
    void deleteBed_successful() {
        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedId(1L)).thenReturn(false);

        bedService.deleteBed(1L);

        verify(bedRepository, times(1)).delete(sampleBed);
    }

    @Test
    void deleteBed_hasAdmissions_throwsException() {
        when(bedRepository.findById(1L)).thenReturn(Optional.of(sampleBed));
        when(admissionRepository.existsByBedId(1L)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                bedService.deleteBed(1L));

        assertTrue(ex.getMessage().contains("admission records"));
        verify(bedRepository, never()).delete(any(Bed.class));
    }

    @Test
    void searchAndFilterBeds_delegatesToRepository() {
        when(bedRepository.searchAndFilterBeds("B-101", "General", BedType.GENERAL, BedStatus.AVAILABLE))
                .thenReturn(Collections.singletonList(sampleBed));

        List<Bed> result = bedService.searchAndFilterBeds("  B-101  ", "  General  ", BedType.GENERAL, BedStatus.AVAILABLE);

        assertEquals(1, result.size());
        verify(bedRepository, times(1)).searchAndFilterBeds("B-101", "General", BedType.GENERAL, BedStatus.AVAILABLE);
    }

    @Test
    void dashboardBedCounts() {
        when(bedRepository.count()).thenReturn(10L);
        when(bedRepository.countByStatus(BedStatus.AVAILABLE)).thenReturn(6L);
        when(bedRepository.countByStatus(BedStatus.OCCUPIED)).thenReturn(3L);
        when(bedRepository.countByStatus(BedStatus.MAINTENANCE)).thenReturn(1L);

        assertEquals(10L, bedService.getTotalBedCount());
        assertEquals(6L, bedService.getAvailableBedCount());
        assertEquals(3L, bedService.getOccupiedBedCount());
        assertEquals(1L, bedService.getMaintenanceBedCount());
    }
}
