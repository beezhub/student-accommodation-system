package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.YearOfStudyNotFoundException;
import com.beezhub.student_accommodation.mapper.YearOfStudyMapper;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.dto.YearOfStudyRequest;
import com.beezhub.student_accommodation.model.dto.YearOfStudyResponse;
import com.beezhub.student_accommodation.model.entity.YearOfStudy;
import com.beezhub.student_accommodation.repository.YearOfStudyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearOfStudyServiceTest {

    @Mock
    private YearOfStudyRepository yearOfStudyRepository;

    @Mock
    private YearOfStudyMapper yearOfStudyMapper;

    @InjectMocks
    private YearOfStudyService yearOfStudyService;

    private YearOfStudy year1;
    private YearOfStudy year2;
    private YearOfStudyResponse response1;
    private YearOfStudyResponse response2;
    private YearOfStudyRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        year1 = new YearOfStudy();
        year1.setId(1L);
        year1.setYearOfStudy("1");

        year2 = new YearOfStudy();
        year2.setId(2L);
        year2.setYearOfStudy("2");

        response1 = new YearOfStudyResponse();
        response1.setId(1L);
        response1.setYearOfStudy("1");

        response2 = new YearOfStudyResponse();
        response2.setId(2L);
        response2.setYearOfStudy("2");

        request = new YearOfStudyRequest();
        request.setYearOfStudy("3");

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getAllYearsOfStudy_ShouldReturnList() {
        // Arrange
        List<YearOfStudy> years = Arrays.asList(year1, year2);
        when(yearOfStudyRepository.findAll()).thenReturn(years);
        when(yearOfStudyMapper.toResponseList(years)).thenReturn(Arrays.asList(response1, response2));

        // Act
        List<YearOfStudyResponse> result = yearOfStudyService.getAllYearsOfStudy();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(yearOfStudyRepository).findAll();
    }

    @Test
    void getAllYearsOfStudy_WithPageable_ShouldReturnPageResponse() {
        // Arrange
        Page<YearOfStudy> page = new PageImpl<>(Arrays.asList(year1, year2), pageable, 2);
        when(yearOfStudyRepository.findAll(pageable)).thenReturn(page);
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);
        when(yearOfStudyMapper.toResponse(year2)).thenReturn(response2);

        // Act
        PageResponse<YearOfStudyResponse> result = yearOfStudyService.getAllYearsOfStudy(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(yearOfStudyRepository).findAll(pageable);
    }

    @Test
    void searchYearsOfStudy_WithYearFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<YearOfStudy> page = new PageImpl<>(Arrays.asList(year1), pageable, 1);
        when(yearOfStudyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);

        // Act
        PageResponse<YearOfStudyResponse> result = yearOfStudyService.searchYearsOfStudy(1, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("1", result.getContent().get(0).getYearOfStudy());
        verify(yearOfStudyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchYearsOfStudy_NoFilter_ShouldReturnAllResults() {
        // Arrange
        Page<YearOfStudy> page = new PageImpl<>(Arrays.asList(year1, year2), pageable, 2);
        when(yearOfStudyRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);
        when(yearOfStudyMapper.toResponse(year2)).thenReturn(response2);

        // Act
        PageResponse<YearOfStudyResponse> result = yearOfStudyService.searchYearsOfStudy(null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(yearOfStudyRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getYearOfStudyById_ShouldReturnYearOfStudy() {
        // Arrange
        when(yearOfStudyRepository.findById(1L)).thenReturn(Optional.of(year1));
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);

        // Act
        YearOfStudyResponse result = yearOfStudyService.getYearOfStudyById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(yearOfStudyRepository).findById(1L);
    }

    @Test
    void getYearOfStudyById_NotFound_ShouldThrowException() {
        // Arrange
        when(yearOfStudyRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(YearOfStudyNotFoundException.class, () -> yearOfStudyService.getYearOfStudyById(1L));
    }

    @Test
    void createYearOfStudy_ShouldSaveAndReturn() {
        // Arrange
        when(yearOfStudyRepository.save(any(YearOfStudy.class))).thenReturn(year1);
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);

        // Act
        YearOfStudyResponse result = yearOfStudyService.createYearOfStudy(request);

        // Assert
        assertNotNull(result);
        verify(yearOfStudyRepository).save(any(YearOfStudy.class));
    }

    @Test
    void updateYearOfStudy_ShouldUpdateAndReturn() {
        // Arrange
        when(yearOfStudyRepository.findById(1L)).thenReturn(Optional.of(year1));
        when(yearOfStudyRepository.save(year1)).thenReturn(year1);
        when(yearOfStudyMapper.toResponse(year1)).thenReturn(response1);

        // Act
        YearOfStudyResponse result = yearOfStudyService.updateYearOfStudy(1L, request);

        // Assert
        assertNotNull(result);
        verify(yearOfStudyRepository).save(year1);
    }

    @Test
    void deleteYearOfStudy_ShouldDelete() {
        // Arrange
        when(yearOfStudyRepository.findById(1L)).thenReturn(Optional.of(year1));

        // Act
        yearOfStudyService.deleteYearOfStudy(1L);

        // Assert
        verify(yearOfStudyRepository).delete(year1);
    }
}
