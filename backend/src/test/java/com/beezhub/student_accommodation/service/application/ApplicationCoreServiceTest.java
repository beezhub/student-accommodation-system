package com.beezhub.student_accommodation.service.application;

import com.beezhub.student_accommodation.exceptions.InvalidStatusException;
import com.beezhub.student_accommodation.exceptions.*;
import com.beezhub.student_accommodation.mapper.ApplicationMapper;
import com.beezhub.student_accommodation.model.dto.*;
import com.beezhub.student_accommodation.model.entity.*;
import com.beezhub.student_accommodation.model.enums.ApplicationStatus;
import com.beezhub.student_accommodation.model.enums.UserRole;
import com.beezhub.student_accommodation.repository.*;
import com.beezhub.student_accommodation.service.AcademicYearService;
import com.beezhub.student_accommodation.service.AuthService;
import com.beezhub.student_accommodation.service.DocumentTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
        private com.beezhub.student_accommodation.repository.DocumentRepository documentRepository;
        @Mock
        private com.beezhub.student_accommodation.repository.DocumentTypeRepository documentTypeRepository;
        @Mock
        private DocumentTypeService documentTypeService;
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
        private Building defaultBuilding;

        @BeforeEach
        void setUp() {
                studentUser = new AppUser();
                studentUser.setId(1L);
                studentUser.setEmail("student@test.com");
                studentUser.setUserRole(UserRole.STUDENT);

                student = new Student();
                student.setId(1L);
                student.setStudentNumber("STU001");
                student.setPhoneNumber("0123456789");
                student.setDateOfBirth(new java.util.Date());
                student.setGender("Female");
                // GH-239: a complete profile now includes an emergency contact, so the happy-path
                // fixture carries one. The gate's own cases below clear it deliberately.
                student.setEmergencyContactName("Thandi Mokoena");
                student.setEmergencyContactPhone("+27 82 123 4567");
                student.setAppUser(studentUser);

                application = Application.builder()
                                .id(1L)
                                .applicationCode("APP-2024-001")
                                .status(ApplicationStatus.PENDING)
                                .student(student)
                                .build();

                defaultBuilding = new Building();
                defaultBuilding.setId(10L);
                defaultBuilding.setName("Block A");
                defaultBuilding.setTenantId(42L);

                applicationRequest = new ApplicationRequest();
                applicationRequest.setStudentId(1L);
                applicationRequest.setRequiresAccessibility(false);
                applicationRequest.setCourseName("Computer Science");
                applicationRequest.setPreferredBuildingIds(List.of(10L));
        }

        @Test
        void submitApplication_shouldCreateApplication() {
                // Arrange
                var academicYear = buildOpenAcademicYear();
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);

                // Act
                ApplicationResponse response = applicationCoreService.submitApplication(applicationRequest);

                // Assert
                assertThat(response).isNotNull();
                verify(applicationRepository).saveAndFlush(any(Application.class));
        }

        @Test
        void submitApplication_shouldThrowException_whenStudentNotFound() {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(StudentNotFoundException.class);
        }

        @Test
        void submitApplication_shouldThrowException_whenDuplicateApplication() {
                var academicYear = buildOpenAcademicYear();
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
                when(documentTypeService.getEffectiveRequiredTypeNames(42L)).thenReturn(Set.of());
                when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(anyLong(), anyLong(), any())).thenReturn(true);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(DuplicateApplicationException.class);
        }

        /**
         * GH-201: the pre-check is a fast path, not the invariant. Two concurrent submits both pass
         * it, so ux_application_student_tenant_active is what actually refuses the second, and the
         * student must see the same sentence either way rather than a 500.
         */
        @Test
        void submitApplication_uniqueIndexViolation_isReportedAsADuplicate() {
                var academicYear = buildOpenAcademicYear();
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
                when(documentTypeService.getEffectiveRequiredTypeNames(anyLong())).thenReturn(Set.of());
                when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(anyLong(), anyLong(), any()))
                                .thenReturn(false);
                when(applicationRepository.nextApplicationCodeSequence()).thenReturn(1L);
                when(applicationRepository.saveAndFlush(any(Application.class))).thenThrow(
                                new DataIntegrityViolationException(
                                                "could not execute statement",
                                                new RuntimeException("ERROR: duplicate key value violates unique "
                                                                + "constraint \"ux_application_student_tenant_active\"")));

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(DuplicateApplicationException.class)
                                .hasMessage("You already have an active application with this property company");
        }

        @Test
        void submitApplication_unrelatedConstraintViolation_isNotDisguisedAsADuplicate() {
                // Reporting every integrity violation as a duplicate application would tell the
                // student to look at an application that has nothing to do with the failure.
                var academicYear = buildOpenAcademicYear();
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
                when(documentTypeService.getEffectiveRequiredTypeNames(anyLong())).thenReturn(Set.of());
                when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(anyLong(), anyLong(), any()))
                                .thenReturn(false);
                when(applicationRepository.nextApplicationCodeSequence()).thenReturn(1L);
                when(applicationRepository.saveAndFlush(any(Application.class))).thenThrow(
                                new DataIntegrityViolationException(
                                                "ERROR: null value in column \"academic_year_id\" violates not-null constraint"));

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void getApplicationById_shouldReturnApplication() {
                when(applicationRepository.findWithDetailsById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);
                when(applicationMapper.toResponse(application)).thenReturn(new ApplicationResponse());

                ApplicationResponse response = applicationCoreService.getApplicationById(1L);

                assertThat(response).isNotNull();
        }

        @Test
        /**
         * GH-156: this deleted the row. That destroyed a record a student and an administrator may
         * both need to refer to, and left the activity log pointing at an id that no longer
         * resolves.
         */
        void cancelApplication_marksItCancelledRatherThanDeletingIt() {
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
                verify(applicationRepository).save(application);
                verify(applicationRepository, never()).delete(any(Application.class));
        }

        /**
         * The old rule was PENDING only, which contradicted the state machine: a student asked to
         * amend their application could not withdraw it instead.
         */
        @Test
        void cancelApplication_changesRequested_isAllowed() {
                application.setStatus(ApplicationStatus.CHANGES_REQUESTED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationCoreService.cancelApplication(1L);

                assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        /**
         * Narrower than the state machine allows, deliberately: by APPROVED an offer may exist, and
         * cancelling the application without releasing it would leave the room counted as occupied
         * with nothing pointing at it. Declining the offer is the route for that.
         */
        @Test
        void cancelApplication_afterApproval_isRefused() {
                application.setStatus(ApplicationStatus.APPROVED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.cancelApplication(1L))
                                .isInstanceOf(InvalidStatusException.class);
                verify(applicationRepository, never()).save(any(Application.class));
        }

        @Test
        void cancelApplication_alreadyCancelled_isRefused() {
                application.setStatus(ApplicationStatus.CANCELLED);
                when(applicationRepository.findById(1L)).thenReturn(Optional.of(application));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.cancelApplication(1L))
                                .isInstanceOf(InvalidStatusException.class);
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
                when(academicYearService.getCurrentAcademicYearEntity()).thenReturn(academicYear);
                when(academicYearService.isApplicationWindowOpen(academicYear)).thenReturn(true);
                lenient().when(applicationRepository.nextApplicationCodeSequence()).thenReturn(1L);
                lenient().when(documentRepository.findDocumentTypeNamesByStudentId(1L)).thenReturn(Set.of());
                lenient().when(documentTypeService.getEffectiveRequiredTypeNames(anyLong())).thenReturn(Set.of());
                lenient().when(applicationRepository.existsByStudent_IdAndTenantIdAndStatusIn(anyLong(), anyLong(), any())).thenReturn(false);
                when(applicationRepository.saveAndFlush(any(Application.class))).thenReturn(application);
                when(applicationMapper.toResponse(any(Application.class))).thenReturn(new ApplicationResponse());
        }

        /**
         * GH-143: the code used to come from SELECT MAX(application_code) + 1 read back through the
         * tenant filter, which raced on concurrent submits, wrapped around lexicographically past 999
         * and saw only one tenant's rows. It now comes from a database sequence.
         *
         * <p>What this test can and cannot show: it pins the format and proves the service consults
         * the sequence exactly once and reads no application row to build the code. The three original
         * defects are prevented by the DELETION of findMaxApplicationCodeByPrefix, which the compiler
         * enforces, and the atomicity itself is only provable against Postgres.
         */
        @Test
        void submitApplication_codeComesFromTheSequence() {
                var academicYear = buildOpenAcademicYear();
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);
                when(applicationRepository.nextApplicationCodeSequence()).thenReturn(7L);

                applicationCoreService.submitApplication(applicationRequest);

                ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
                verify(applicationRepository).saveAndFlush(saved.capture());
                assertThat(saved.getValue().getApplicationCode())
                                .isEqualTo("APP-" + java.time.LocalDate.now().getYear() + "-007");
                verify(applicationRepository, times(1)).nextApplicationCodeSequence();
        }

        /**
         * Padding stays at three digits so new codes match every code already issued. Past 999 the
         * number simply renders wider, which is harmless now that nothing parses or compares a code.
         * Under the old generator a fourth digit was what poisoned the counter for the rest of the year.
         */
        @Test
        void submitApplication_sequenceBeyond999_rendersWiderWithoutTruncation() {
                var academicYear = buildOpenAcademicYear();
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);
                when(applicationRepository.nextApplicationCodeSequence()).thenReturn(12345L);

                applicationCoreService.submitApplication(applicationRequest);

                ArgumentCaptor<Application> saved = ArgumentCaptor.forClass(Application.class);
                verify(applicationRepository).saveAndFlush(saved.capture());
                assertThat(saved.getValue().getApplicationCode())
                                .isEqualTo("APP-" + java.time.LocalDate.now().getYear() + "-12345");
        }

        @Test
        void submitApplication_withOnePreferredBuilding_savesOnePreference() {
                var academicYear = buildOpenAcademicYear();
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);

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
                Building b1 = new Building(); b1.setId(10L); b1.setName("Block A"); b1.setTenantId(42L);
                Building b2 = new Building(); b2.setId(11L); b2.setName("Block B"); b2.setTenantId(42L);
                Building b3 = new Building(); b3.setId(12L); b3.setName("Block C"); b3.setTenantId(42L);
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(b1));
                when(buildingRepository.findById(11L)).thenReturn(Optional.of(b2));
                when(buildingRepository.findById(12L)).thenReturn(Optional.of(b3));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);

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
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationRequest.setPreferredBuildingIds(List.of(10L, 11L, 12L, 13L));

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessageContaining("3");
        }

        @Test
        void submitApplication_withNoPreferredBuildings_throwsException() {
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                applicationRequest.setPreferredBuildingIds(null);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("building preference is required");
        }

        @Test
        void submitApplication_incompleteProfile_throwsExceptionListingMissingFields() {
                student.setPhoneNumber(null);
                student.setDateOfBirth(null);
                student.setGender(null);
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("profile is incomplete")
                                .hasMessageContaining("phone number")
                                .hasMessageContaining("date of birth")
                                .hasMessageContaining("gender");
        }

        /**
         * GH-239. The emergency contact is required here rather than by a NOT NULL column, because
         * existing students have no value and none can be invented for them. This gate makes it
         * required going forward, for exactly the students it matters for.
         */
        @Test
        void submitApplication_noEmergencyContact_isRefusedAndSaysWhichPartIsMissing() {
                student.setEmergencyContactName(null);
                student.setEmergencyContactPhone(null);
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("profile is incomplete")
                                .hasMessageContaining("emergency contact name")
                                .hasMessageContaining("emergency contact phone");
        }

        /** A name with no number is not a contact: there is no one to call. */
        @Test
        void submitApplication_emergencyContactNameWithoutAPhone_isRefused() {
                student.setEmergencyContactPhone(null);
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("emergency contact phone");
        }

        /** Blank is not a value; the other profile checks treat it the same way. */
        @Test
        void submitApplication_blankEmergencyContact_isRefused() {
                student.setEmergencyContactName("   ");
                when(studentRepository.findStudentWithDetailsById(1L)).thenReturn(Optional.of(student));
                when(authService.getCurrentUser()).thenReturn(studentUser);

                assertThatThrownBy(() -> applicationCoreService.submitApplication(applicationRequest))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("emergency contact name");
        }

        /**
         * Relationship and email are optional. Requiring them would refuse an application over a
         * detail that does not affect whether anyone can be reached.
         */
        @Test
        void submitApplication_emergencyContactWithoutRelationshipOrEmail_isAccepted() {
                student.setEmergencyContactRelationship(null);
                student.setEmergencyContactEmail(null);
                var academicYear = buildOpenAcademicYear();
                when(buildingRepository.findById(10L)).thenReturn(Optional.of(defaultBuilding));
                when(buildingPreferenceRepository.saveAll(any())).thenReturn(List.of());
                stubCommonSubmitMocks(academicYear);

                applicationCoreService.submitApplication(applicationRequest);

                verify(applicationRepository).saveAndFlush(any(Application.class));
        }

}
