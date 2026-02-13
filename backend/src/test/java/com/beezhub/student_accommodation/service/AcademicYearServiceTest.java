package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.AcademicYearNotFoundException;
import com.beezhub.student_accommodation.exceptions.DuplicateResourceException;
import com.beezhub.student_accommodation.mapper.AcademicYearMapper;
import com.beezhub.student_accommodation.model.dto.AcademicYearRequest;
import com.beezhub.student_accommodation.model.dto.AcademicYearResponse;
import com.beezhub.student_accommodation.model.entity.AcademicYear;
import com.beezhub.student_accommodation.repository.AcademicYearRepository;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private AcademicYearMapper academicYearMapper;

    @InjectMocks
    private AcademicYearService academicYearService;

    private AcademicYear academicYear;
    private AcademicYearRequest request;
    private AcademicYearResponse response;

    @BeforeEach
    void setUp() {
        academicYear = AcademicYear.builder()
                .id(1L)
                .name("2026")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .applicationOpenDate(LocalDate.of(2025, 9, 1))
                .applicationCloseDate(LocalDate.of(2026, 1, 31))
                .isCurrent(true)
                .build();

        request = AcademicYearRequest.builder()
                .name("2026")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .applicationOpenDate(LocalDate.of(2025, 9, 1))
                .applicationCloseDate(LocalDate.of(2026, 1, 31))
                .isCurrent(true)
                .build();

        response = AcademicYearResponse.builder()
                .id(1L)
                .name("2026")
                .startDate(LocalDate.of(2026, 2, 1))
                .endDate(LocalDate.of(2026, 11, 30))
                .applicationOpenDate(LocalDate.of(2025, 9, 1))
                .applicationCloseDate(LocalDate.of(2026, 1, 31))
                .isCurrent(true)
                .isApplicationWindowOpen(false)
                .build();
    }

    @Test
    void createAcademicYear_shouldCreateSuccessfully() {
        when(academicYearRepository.existsByName("2026")).thenReturn(false);
        when(academicYearMapper.toEntity(request)).thenReturn(academicYear);
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(academicYear);
        when(academicYearMapper.toResponse(academicYear)).thenReturn(response);

        AcademicYearResponse result = academicYearService.createAcademicYear(request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("2026");
        verify(academicYearRepository).clearCurrentAcademicYear();
        verify(academicYearRepository).save(any(AcademicYear.class));
    }

    @Test
    void createAcademicYear_shouldThrowException_whenNameExists() {
        when(academicYearRepository.existsByName("2026")).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.createAcademicYear(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getAcademicYearById_shouldReturnAcademicYear() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(academicYearMapper.toResponse(academicYear)).thenReturn(response);

        AcademicYearResponse result = academicYearService.getAcademicYearById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getAcademicYearById_shouldThrowException_whenNotFound() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.getAcademicYearById(1L))
                .isInstanceOf(AcademicYearNotFoundException.class);
    }

    @Test
    void getCurrentAcademicYear_shouldReturnCurrentYear() {
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.of(academicYear));
        when(academicYearMapper.toResponse(academicYear)).thenReturn(response);

        AcademicYearResponse result = academicYearService.getCurrentAcademicYear();

        assertThat(result).isNotNull();
        assertThat(result.getIsCurrent()).isTrue();
    }

    @Test
    void getCurrentAcademicYear_shouldThrowException_whenNoCurrent() {
        when(academicYearRepository.findByIsCurrentTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> academicYearService.getCurrentAcademicYear())
                .isInstanceOf(AcademicYearNotFoundException.class)
                .hasMessageContaining("No current academic year");
    }

    @Test
    void setCurrentAcademicYear_shouldSetAsCurrentAndClearOthers() {
        AcademicYear notCurrent = AcademicYear.builder()
                .id(2L)
                .name("2027")
                .isCurrent(false)
                .build();

        when(academicYearRepository.findById(2L)).thenReturn(Optional.of(notCurrent));
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(notCurrent);
        when(academicYearMapper.toResponse(any(AcademicYear.class))).thenReturn(
                AcademicYearResponse.builder().id(2L).isCurrent(true).build());

        AcademicYearResponse result = academicYearService.setCurrentAcademicYear(2L);

        verify(academicYearRepository).clearCurrentAcademicYear();
        verify(academicYearRepository).save(any(AcademicYear.class));
        assertThat(result.getIsCurrent()).isTrue();
    }

    @Test
    void deleteAcademicYear_shouldDelete_whenNoApplicationsLinked() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(applicationRepository.existsByAcademicYear_Id(1L)).thenReturn(false);

        academicYearService.deleteAcademicYear(1L);

        verify(academicYearRepository).delete(academicYear);
    }

    @Test
    void deleteAcademicYear_shouldThrowException_whenApplicationsLinked() {
        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(applicationRepository.existsByAcademicYear_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> academicYearService.deleteAcademicYear(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete");
    }

    @Test
    void isApplicationWindowOpen_shouldReturnTrue_whenWithinWindow() {
        AcademicYear windowOpen = AcademicYear.builder()
                .applicationOpenDate(LocalDate.now().minusDays(10))
                .applicationCloseDate(LocalDate.now().plusDays(10))
                .build();

        boolean result = academicYearService.isApplicationWindowOpen(windowOpen);

        assertThat(result).isTrue();
    }

    @Test
    void isApplicationWindowOpen_shouldReturnFalse_whenBeforeWindow() {
        AcademicYear windowNotOpen = AcademicYear.builder()
                .applicationOpenDate(LocalDate.now().plusDays(10))
                .applicationCloseDate(LocalDate.now().plusDays(30))
                .build();

        boolean result = academicYearService.isApplicationWindowOpen(windowNotOpen);

        assertThat(result).isFalse();
    }

    @Test
    void isApplicationWindowOpen_shouldReturnFalse_whenAfterWindow() {
        AcademicYear windowClosed = AcademicYear.builder()
                .applicationOpenDate(LocalDate.now().minusDays(30))
                .applicationCloseDate(LocalDate.now().minusDays(10))
                .build();

        boolean result = academicYearService.isApplicationWindowOpen(windowClosed);

        assertThat(result).isFalse();
    }

    @Test
    void isApplicationWindowOpen_shouldReturnFalse_whenNullDates() {
        AcademicYear nullDates = AcademicYear.builder().build();

        boolean result = academicYearService.isApplicationWindowOpen(nullDates);

        assertThat(result).isFalse();
    }

    @Test
    void getAllAcademicYears_shouldReturnList() {
        when(academicYearRepository.findAll()).thenReturn(List.of(academicYear));
        when(academicYearMapper.toResponse(any(AcademicYear.class))).thenReturn(response);

        List<AcademicYearResponse> results = academicYearService.getAllAcademicYears();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("2026");
    }

    @Test
    void updateAcademicYear_shouldUpdateSuccessfully() {
        AcademicYearRequest updateRequest = AcademicYearRequest.builder()
                .name("2026")
                .startDate(LocalDate.of(2026, 3, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .applicationOpenDate(LocalDate.of(2025, 10, 1))
                .applicationCloseDate(LocalDate.of(2026, 2, 28))
                .isCurrent(true)
                .build();

        when(academicYearRepository.findById(1L)).thenReturn(Optional.of(academicYear));
        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(academicYear);
        when(academicYearMapper.toResponse(any(AcademicYear.class))).thenReturn(response);

        AcademicYearResponse result = academicYearService.updateAcademicYear(1L, updateRequest);

        assertThat(result).isNotNull();
        verify(academicYearMapper).updateEntity(eq(updateRequest), any(AcademicYear.class));
    }
}
