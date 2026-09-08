package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.ApplicationResponse;
import com.beezhub.student_accommodation.model.dto.PageResponse;
import com.beezhub.student_accommodation.model.entity.AppUser;
import com.beezhub.student_accommodation.model.entity.Application;
import com.beezhub.student_accommodation.model.entity.Student;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.ApplicationRepository;
import com.beezhub.student_accommodation.repository.BuildingRepository;
import com.beezhub.student_accommodation.repository.DocumentRepository;
import com.beezhub.student_accommodation.repository.DocumentTypeRepository;
import com.beezhub.student_accommodation.repository.StagedAllocationRepository;
import com.beezhub.student_accommodation.repository.StudentRepository;
import com.beezhub.student_accommodation.service.DocumentTypeService;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationSearchServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private StagedAllocationRepository stagedAllocationRepository;

    @Mock
    private DocumentTypeService documentTypeService;

    @InjectMocks
    private ApplicationSearchService applicationSearchService;

    private Application application1;
    private Application application2;
    private Application application3;
    private ApplicationResponse response1;
    private ApplicationResponse response2;
    private ApplicationResponse response3;
    private Student student1;
    private Student student2;
    private AppUser appUser1;
    private AppUser appUser2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        appUser1 = AppUser.builder()
                .id(1L)
                .email("student1@test.com")
                .firstName("John")
                .lastName("Doe")
                .userRole(UserRole.STUDENT)
                .build();

        appUser2 = AppUser.builder()
                .id(2L)
                .email(" student2@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .userRole(UserRole.STUDENT)
                .build();

        student1 = new Student();
        student1.setId(1L);
        student1.setStudentNumber("STU001");
        student1.setAppUser(appUser1);

        student2 = new Student();
        student2.setId(2L);
        student2.setStudentNumber("STU002");
        student2.setAppUser(appUser2);

        application1 = Application.builder()
                .id(1L)
                .applicationCode("APP-2025-001")
                .applicationDate(LocalDate.of(2025, 1, 15))
                .status(ApplicationStatus.PENDING)
                .student(student1)
                .build();

        application2 = Application.builder()
                .id(2L)
                .applicationCode("APP-2025-002")
                .applicationDate(LocalDate.of(2025, 2, 20))
                .status(ApplicationStatus.APPROVED)
                .student(student2)
                .build();

        application3 = Application.builder()
                .id(3L)
                .applicationCode("APP-2025-003")
                .applicationDate(LocalDate.of(2025, 3, 10))
                .status(ApplicationStatus.REJECTED)
                .student(student1)
                .build();

        response1 = new ApplicationResponse();
        response1.setId(1L);
        response1.setApplicationCode("APP-2025-001");
        response1.setApplicationDate(LocalDate.of(2025, 1, 15));
        response1.setStatus("PENDING");

        response2 = new ApplicationResponse();
        response2.setId(2L);
        response2.setApplicationCode("APP-2025-002");
        response2.setApplicationDate(LocalDate.of(2025, 2, 20));
        response2.setStatus("APPROVED");

        response3 = new ApplicationResponse();
        response3.setId(3L);
        response3.setApplicationCode("APP-2025-003");
        response3.setApplicationDate(LocalDate.of(2025, 3, 10));
        response3.setStatus("REJECTED");

        pageable = PageRequest.of(0, 20);

        // Mock document repository for enrichApplicationDetails
        when(documentRepository.findAllByStudent_IdIn(any())).thenReturn(List.of());
        when(documentTypeService.getEffectiveRequiredTypeNames(any())).thenReturn(java.util.Set.of());
        when(stagedAllocationRepository.findByApplication_IdIn(any())).thenReturn(List.of());
    }

    @Test
    void getAllApplications_WithPageable_ShouldReturnPageResponse() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1, application2), pageable, 2);
        when(applicationRepository.findAll(pageable)).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.getAllApplications(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(applicationRepository).findAll(pageable);
    }

    @Test
    void searchApplications_WithApplicationCodeFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                "APP-2025-001", null, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("APP-2025-001", result.getContent().get(0).getApplicationCode());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithStudentNameFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1, application3), pageable, 2);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);
        when(applicationMapper.toResponse(application3)).thenReturn(response3);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, "John", null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithStatusFilter_ShouldReturnFilteredResults() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, ApplicationStatus.PENDING, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("PENDING", result.getContent().get(0).getStatus());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithDateRangeFilter_ShouldReturnFilteredResults() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 2, 28);
        Page<Application> page = new PageImpl<>(Arrays.asList(application2), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, null, null, null, startDate, endDate, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertTrue(result.getContent().get(0).getApplicationDate().isAfter(startDate.minusDays(1)));
        assertTrue(result.getContent().get(0).getApplicationDate().isBefore(endDate.plusDays(1)));
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithStartDateOnly_ShouldReturnFilteredResults() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 2, 15);
        Page<Application> page = new PageImpl<>(Arrays.asList(application2, application3), pageable, 2);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);
        when(applicationMapper.toResponse(application3)).thenReturn(response3);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, null, null, null, startDate, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithEndDateOnly_ShouldReturnFilteredResults() {
        // Arrange
        LocalDate endDate = LocalDate.of(2025, 2, 25);
        Page<Application> page = new PageImpl<>(Arrays.asList(application1, application2), pageable, 2);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, null, null, null, null, endDate, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithMultipleFilters_ShouldReturnFilteredResults() {
        // Arrange
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        Page<Application> page = new PageImpl<>(Arrays.asList(application1), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                "APP-2025", "John", ApplicationStatus.PENDING, null, null, startDate, endDate,
                pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_NoFilters_ShouldReturnAllResults() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1, application2, application3),
                pageable, 3);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);
        when(applicationMapper.toResponse(application3)).thenReturn(response3);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithApprovedStatus_ShouldReturnOnlyApproved() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application2), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, ApplicationStatus.APPROVED, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("APPROVED", result.getContent().get(0).getStatus());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_WithRejectedStatus_ShouldReturnOnlyRejected() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application3), pageable, 1);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application3)).thenReturn(response3);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                null, null, ApplicationStatus.REJECTED, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("REJECTED", result.getContent().get(0).getStatus());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void searchApplications_PartialCodeMatch_ShouldReturnMatchingResults() {
        // Arrange
        Page<Application> page = new PageImpl<>(Arrays.asList(application1, application2, application3),
                pageable, 3);
        when(applicationRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(applicationMapper.toResponse(application1)).thenReturn(response1);
        when(applicationMapper.toResponse(application2)).thenReturn(response2);
        when(applicationMapper.toResponse(application3)).thenReturn(response3);

        // Act
        PageResponse<ApplicationResponse> result = applicationSearchService.searchApplications(
                "2025", null, null, null, null, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        verify(applicationRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getApplicationsByStatus_ShouldReturnListOfApplicationsWithSpecificStatus() {
        // Arrange
        when(applicationRepository.findByStatus(ApplicationStatus.PENDING))
                .thenReturn(Arrays.asList(application1));
        when(applicationMapper.toResponse(application1)).thenReturn(response1);

        // Act
        List<ApplicationResponse> result = applicationSearchService.getApplicationsByStatus("PENDING");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getStatus());
        verify(applicationRepository).findByStatus(ApplicationStatus.PENDING);
    }
}
