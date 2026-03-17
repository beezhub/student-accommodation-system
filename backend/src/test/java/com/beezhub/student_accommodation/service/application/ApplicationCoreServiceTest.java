package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationCoreServiceTest {

        @Mock
        private ApplicationRepository applicationRepository;
        @Mock
        private StudentRepository studentRepository;
        @Mock
        private BuildingRepository buildingRepository;
        @Mock
        private ApplicationBuildingPreferenceRepository buildingPreferenceRepository;
        @Mock
        private ApplicationMapper applicationMapper;
        @Mock
        private AuthService authService;
        @Mock
        private AcademicYearService academicYearService;

        @InjectMocks
        private ApplicationCoreService applicationCoreService;

        private Student student;
        private AppUser studentUser;
        private Application application;
        private ApplicationRequest applicationRequest;

        @BeforeEach
        void setUp() {
                studentUser = new AppUser();
                studentUser.setId(1L);
                studentUser.setEmail("student@test.com");
                studentUser.setUserRole(UserRole.STUDENT);

                student = new Student();
                student.setId(1L);
                student.setStudentNumber("STU001");
                student.setAppUser(studentUser);

                application = Application.builder()
                                .id(1L)
                                .applicationCode("APP-2024-001")
                                .status(ApplicationStatus.PENDING)
                                .student(student)
                                .build();

                applicationRequest = new ApplicationRequest();
                applicationRequest.setStudentId(1L);
                applicationRequest.setRequiresAccessibility(false);
                applicationRequest.setCourseName("Computer Science");
        }

        @Test
        void submitApplication_shouldCreateApplication() {
                // Arrange
                com.beezhub.student_accommodation.model.entity.AcademicYear academicYear = com.beezhub.student_accommodation.model.entity.AcademicYear
                                .builder()
                                .id(1L)
                                .name("2025/2026")
                                .startDate(LocalDate.now().minusMonths(2))
                                .endDate(LocalDate.now().plusMonths(10))
                                .applicationOpenDate(LocalDate.now().minusDays(10))
                                .applicationCloseDate(LocalDate.now().plusDays(30))
                                .isCurrent(true)
                                .build();

                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING))
                                .thenReturn(false);
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                lenient().when(applicationRepository.countByApplicationCodeStartingWith(anyString())).thenReturn(0L);
                when(applicationRepository.save(any(Application.class))).thenReturn(application);
                when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());

                // Act
                ApplicationResponse response = applicationCoreService.submitApplication(applicationRequest);

                // Assert
                assertThat(response).isNotNull();
                verify(applicationRepository).save(any(Application.class));
        }

        @Test
        void submitApplication_shouldThrowException_whenStudentNotFound() {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(StudentNotFoundException.class);
        }

        @Test
        void submitApplication_shouldThrowException_whenDuplicateApplication() {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(true);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(DuplicateApplicationException.class);
        }

        @Test
        void getApplicationById_shouldReturnApplication() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

                ApplicationResponse response = applicationCoreService.getApplicationById(1L);

                assertThat(response).isNotNull();
        }

        @Test
        void cancelApplication_shouldDeleteApplication() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                verify(applicationRepository).delete(application);
        }

        @Test
        void cancelApplication_shouldThrowException_whenNotPending() {
                application.setStatus(ApplicationStatus.APPROVED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.cancelApplication(1L))
                                .isInstanceOf(IllegalStateException.class);
        }

        // ── Building preference tests ─────────────────────────────────────────────

        private com.beezhub.student_accommodation.model.entity.AcademicYear buildOpenAcademicYear() {
                return com.beezhub.student_accommodation.model.entity.AcademicYear.builder()
                                .id(1L)
                                .name("2025/2026")
                                .startDate(LocalDate.now().minusMonths(2))
                                .endDate(LocalDate.now().plusMonths(10))
                                .applicationOpenDate(LocalDate.now().minusDays(10))
                                .applicationCloseDate(LocalDate.now().plusDays(30))
                                .isCurrent(true)
                                .build();
        }

        private void stubCommonSubmitMocks(com.beezhub.student_accommodation.model.entity.AcademicYear academicYear) {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(false);
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                lenient().when(applicationRepository.countByApplicationCodeStartingWith(anyString())).thenReturn(0L);
                when(applicationRepository.save(any(Application.class))).thenReturn(application);
                when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());
        }

        @Test
        void submitApplication_withOnePreferredBuilding_savesOnePreference() {
                var academicYear = buildOpenAcademicYear();
                stubCommonSubmitMocks(academicYear);

                Building building1 = new Building();
                building1.setId(10L);
                building1.setName("Block A");
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(building1));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());

                applicationRequest.setPreferredBuildingIds(List.of(10L));

                ApplicationResponse response = applicationCoreService.submitApplication(applicationRequest);

                assertThat(response).isNotNull();
                verify(buildingPreferenceRepository).saveAll(argThat(prefs -> {
                        var list = (List<?>) prefs;
                        return list.size() == 1;
                }));
        }

        @Test
        void submitApplication_withThreePreferredBuildings_savesThreePreferences() {
                var academicYear = buildOpenAcademicYear();
                stubCommonSubmitMocks(academicYear);

                Building b1 = new Building(); b1.setId(10L); b1.setName("Block A");
                Building b2 = new Building(); b2.setId(11L); b2.setName("Block B");
                Building b3 = new Building(); b3.setId(12L); b3.setName("Block C");
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(b1));
                when(buildingRepository.findById(11L)).thenReturn(Optional.of(b2));
                when(buildingRepository.findById(12L)).thenReturn(Optional.of(b3));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());

                applicationRequest.setPreferredBuildingIds(List.of(10L, 11L, 12L));

                ApplicationResponse response = applicationCoreService.submitApplication(applicationRequest);

                assertThat(response).isNotNull();
                verify(buildingPreferenceRepository).saveAll(argThat(prefs -> {
                        var list = (List<?>) prefs;
                        return list.size() == 3;
                }));
        }

        @Test
        void submitApplication_withFourPreferredBuildings_throwsException() {
                var academicYear = buildOpenAcademicYear();
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationRepository.existsByStudent_IdAndStatus(1L, ApplicationStatus.PENDING)).thenReturn(false);
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);

                applicationRequest.setPreferredBuildingIds(List.of(10L, 11L, 12L, 13L));

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("3");
        }

        @Test
        void submitApplication_withNoPreferredBuildings_doesNotSavePreferences() {
                var academicYear = buildOpenAcademicYear();
                stubCommonSubmitMocks(academicYear);

                applicationRequest.setPreferredBuildingIds(null);

                ApplicationResponse response = applicationCoreService.submitApplication(applicationRequest);

                assertThat(response).isNotNull();
                verifyNoInteractions(buildingPreferenceRepository);
        }

}
