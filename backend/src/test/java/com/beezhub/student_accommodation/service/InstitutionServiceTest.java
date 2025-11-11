package com.beezhub.student_accommodation.service;

import com.beezhub.student_accommodation.exceptions.InstitutionNotFoundException;
import com.beezhub.student_accommodation.mapper.InstitutionMapper;
import com.beezhub.student_accommodation.model.dto.InstitutionRequest;
import com.beezhub.student_accommodation.model.dto.InstitutionResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.Institution;
import com.beezhub.student_accommodation.repository.InstitutionRepository;
import com.beezhub.student_accommodation.specification.InstitutionSpecification;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceTest {

    @Mock
    private InstitutionRepository institutionRepository;

    @Mock
    private InstitutionMapper institutionMapper;

    @InjectMocks
    private InstitutionService institutionService;

    private Institution institution1;
    private Institution institution2;
    private InstitutionResponse response1;
    private InstitutionResponse response2;
    private InstitutionRequest request;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        institution1 = new Institution();
        institution1.setId(1L);
        institution1.setName("University of Test");
        institution1.setAddress("123 Test St");
        institution1.setEmailAddress("test@university.com");

        institution2 = new Institution();
        institution2.setId(2L);
        institution2.setName("College of Science");
        institution2.setAddress("456 Science Ave");
        institution2.setEmailAddress("info@college.com");

        response1 = new InstitutionResponse();
        response1.setId("1");
        response1.setName("University of Test");
        response1.setAddress("123 Test St");
        response1.setEmailAddress("test@university.com");

        response2 = new InstitutionResponse();
        response2.setId("2");
        response2.setName("College of Science");
        response2.setAddress("456 Science Ave");
        response2.setEmailAddress("info@college.com");

        request = new InstitutionRequest();
        request.setName("New Institution");
        request.setAddress("789 New St");
        request.setPhoneNumber("123-456-7890");
        request.setEmailAddress("new@institution.com");

        pageable = PageRequest.of(0, 20);
    }

    @Test
    void getAllInstitutions_ShouldReturnList() {
        // Arrange
        List<Institution> institutions = Arrays.asList(institution1, institution2);
        when(institutionRepository.findAll()).thenReturn(institutions);
        when(institutionMapper.toResponseList(institutions)).thenReturn(Arrays.asList(response1, response2));

        // Act
        List<InstitutionResponse> result = institutionService.getAllInstitutions();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(institutionRepository).findAll();
        verify(institutionMapper).toResponseList(institutions);
    }

    @Test
    void getAllInstitutions_WithPageable_ShouldReturnPageResponse() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1, institution2), pageable, 2);
        when(institutionRepository.findAll(pageable)).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);
        when(institutionMapper.toResponse(institution2)).thenReturn(response2);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.getAllInstitutions(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        verify(institutionRepository).findAll(pageable);
    }

    @Test
    void searchInstitutions_WithNameFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1), pageable, 1);
        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.searchInstitutions("University", null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("University of Test", result.getContent().get(0).getName());
        verify(institutionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchInstitutions_WithAddressFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1), pageable, 1);
        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.searchInstitutions(null, "Test St", null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(institutionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchInstitutions_WithContactEmailFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1), pageable, 1);
        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.searchInstitutions(null, null, "test@university", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(institutionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchInstitutions_WithMultipleFilters_ShouldReturnFilteredResults() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1), pageable, 1);
        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.searchInstitutions("University", "Test", "test@", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(institutionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchInstitutions_NoFilters_ShouldReturnAllResults() {
        // Arrange
        Page<Institution> page = new PageImpl<>(Arrays.asList(institution1, institution2), pageable, 2);
        when(institutionRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);
        when(institutionMapper.toResponse(institution2)).thenReturn(response2);

        // Act
        PageResponse<InstitutionResponse> result = institutionService.searchInstitutions(null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(institutionRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getInstitutionById_ShouldReturnInstitution() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution1));
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        InstitutionResponse result = institutionService.getInstitutionById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("1", result.getId());
        verify(institutionRepository).findById(1L);
    }

    @Test
    void getInstitutionById_NotFound_ShouldThrowException() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InstitutionNotFoundException.class, () -> institutionService.getInstitutionById(1L));
        verify(institutionRepository).findById(1L);
    }

    @Test
    void createInstitution_ShouldSaveAndReturnInstitution() {
        // Arrange
        when(institutionRepository.save(any(Institution.class))).thenReturn(institution1);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        InstitutionResponse result = institutionService.createInstitution(request);

        // Assert
        assertNotNull(result);
        verify(institutionRepository).save(any(Institution.class));
    }

    @Test
    void updateInstitution_ShouldUpdateAndReturnInstitution() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution1));
        when(institutionRepository.save(institution1)).thenReturn(institution1);
        when(institutionMapper.toResponse(institution1)).thenReturn(response1);

        // Act
        InstitutionResponse result = institutionService.updateInstitution(1L, request);

        // Assert
        assertNotNull(result);
        verify(institutionRepository).findById(1L);
        verify(institutionRepository).save(institution1);
    }

    @Test
    void deleteInstitution_ShouldDeleteInstitution() {
        // Arrange
        when(institutionRepository.findById(1L)).thenReturn(Optional.of(institution1));

        // Act
        institutionService.deleteInstitution(1L);

        // Assert
        verify(institutionRepository).findById(1L);
        verify(institutionRepository).delete(institution1);
    }
}
